package com.example.r2dbc

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.data.relational.core.mapping.Table
import org.springframework.stereotype.Service
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@SpringBootApplication
class R2dbcWebfluxApplication

fun main(args: Array<String>) {
    runApplication<R2dbcWebfluxApplication>(*args)
}

// Spring Boot auto-configures a ReactiveTransactionManager (R2dbcTransactionManager)
// but NOT a TransactionalOperator bean, so we declare it.
@Configuration
class TxConfig {
    @Bean
    fun transactionalOperator(tm: ReactiveTransactionManager) = TransactionalOperator.create(tm)
}

// ---------------------------------------------------------------------------
// Domain
// ---------------------------------------------------------------------------

enum class WorkStatus { PENDING, SUCCESS, FAILED }

@Table("work_log")
class WorkLog(
    var status: WorkStatus,
    var detail: String,
    @Id var id: Long? = null,
)

interface WorkLogRepository : R2dbcRepository<WorkLog, Long>

// ---------------------------------------------------------------------------
// Fake external API. Errors reactively when shouldFail = true.
// ---------------------------------------------------------------------------

@Service
class ExternalApiClient {
    fun call(shouldFail: Boolean): Mono<String> =
        if (shouldFail) Mono.error(RuntimeException("external API 5xx")) else Mono.just("OK")
}

// ---------------------------------------------------------------------------
// BROKEN: the whole chain -- including the attempt to record FAILED -- runs
// inside ONE transactional boundary, and the chain ultimately errors. The
// TransactionalOperator sees a terminal error signal at the boundary and rolls
// back EVERYTHING, so the FAILED write never survives.
// ---------------------------------------------------------------------------

@Service
class BrokenService(
    private val repo: WorkLogRepository,
    private val api: ExternalApiClient,
    private val tx: TransactionalOperator,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun process(shouldFail: Boolean): Mono<Long> {
        val chain = repo.save(WorkLog(WorkStatus.PENDING, "started"))
            .flatMap { saved -> api.call(shouldFail).thenReturn(saved) }
            .flatMap { saved ->
                saved.status = WorkStatus.SUCCESS
                saved.detail = "api ok"
                repo.save(saved)
            }
            .map { it.id!! }
            .onErrorResume { e ->
                // record FAILED inside the SAME tx, then rethrow
                log.warn("caught {} -> recording FAILED in same tx (will be rolled back)", e.message)
                repo.save(WorkLog(WorkStatus.FAILED, "failed: ${e.message}"))
                    .then(Mono.error(e))
            }
        // error reaches the boundary -> rollback of PENDING and FAILED alike
        return chain.`as`(tx::transactional)
    }
}

// ---------------------------------------------------------------------------
// FIXED: tx #1 wraps only the API-call-and-success path. If it errors, tx #1
// rolls back; THEN, downstream of that boundary, we open tx #2 to commit the
// FAILED record independently.
// ---------------------------------------------------------------------------

@Service
class FixedService(
    private val repo: WorkLogRepository,
    private val api: ExternalApiClient,
    private val tx: TransactionalOperator,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun process(shouldFail: Boolean): Mono<Long> {
        val tx1 = repo.save(WorkLog(WorkStatus.PENDING, "started"))
            .flatMap { saved -> api.call(shouldFail).thenReturn(saved) }
            .flatMap { saved ->
                saved.status = WorkStatus.SUCCESS
                saved.detail = "api ok"
                repo.save(saved)
            }
            .map { it.id!! }
            .`as`(tx::transactional) // tx #1 -- rolls back on error

        return tx1.onErrorResume { e ->
            log.warn("caught {} -> recording FAILED in a new transaction", e.message)
            repo.save(WorkLog(WorkStatus.FAILED, "failed: ${e.message}"))
                .map { it.id!! }
                .`as`(tx::transactional) // tx #2 -- commits independently
        }
    }
}

// ---------------------------------------------------------------------------
// HTTP surface
// ---------------------------------------------------------------------------

@RestController
class DemoController(
    private val broken: BrokenService,
    private val fixed: FixedService,
    private val repo: WorkLogRepository,
) {
    data class RowView(val id: Long?, val status: WorkStatus, val detail: String)
    data class Result(val mode: String, val fail: Boolean, val savedId: Long?, val rows: List<RowView>, val error: String?)

    @PostMapping("/broken")
    fun broken(@RequestParam(defaultValue = "true") fail: Boolean): Mono<Result> = run("broken", fail, broken.process(fail))

    @PostMapping("/fixed")
    fun fixed(@RequestParam(defaultValue = "true") fail: Boolean): Mono<Result> = run("fixed", fail, fixed.process(fail))

    @GetMapping("/logs")
    fun logs(): Mono<List<RowView>> = repo.findAll().map { RowView(it.id, it.status, it.detail) }.collectList()

    private fun run(mode: String, fail: Boolean, action: Mono<Long>): Mono<Result> =
        action.map { id -> Pair<Long?, String?>(id, null) }
            .onErrorResume { e -> Mono.just(Pair<Long?, String?>(null, "${e.javaClass.simpleName}: ${e.message}")) }
            .flatMap { (id, err) ->
                repo.findAll().map { RowView(it.id, it.status, it.detail) }.collectList()
                    .map { rows -> Result(mode, fail, id, rows, err) }
            }
}
