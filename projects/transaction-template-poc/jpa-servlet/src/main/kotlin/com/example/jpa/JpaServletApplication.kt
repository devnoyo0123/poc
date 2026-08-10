package com.example.jpa

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.web.client.RestClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class JpaServletApplication

fun main(args: Array<String>) {
    runApplication<JpaServletApplication>(*args)
}

// Spring Boot auto-configures a PlatformTransactionManager but NOT a
// TransactionTemplate bean, so we declare it explicitly.
@Configuration
class TxConfig {
    @Bean
    fun transactionTemplate(tm: PlatformTransactionManager) = TransactionTemplate(tm)
}

// ---------------------------------------------------------------------------
// Domain: a record of one "external API call + persist" attempt.
// ---------------------------------------------------------------------------

enum class WorkStatus { PENDING, SUCCESS, FAILED }

@Entity
class WorkLog(
    @Enumerated(EnumType.STRING)
    var status: WorkStatus = WorkStatus.PENDING,
    var detail: String = "",
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

interface WorkLogRepository : JpaRepository<WorkLog, Long>

// ---------------------------------------------------------------------------
// Fake external API. Throws when shouldFail = true.
// ---------------------------------------------------------------------------

interface ExternalApiClient {
    fun call(shouldFail: Boolean): String
}

// Default impl for unit tests / local run: no network, throws on shouldFail.
@Component
@Profile("!scenario")
class FakeExternalApiClient : ExternalApiClient {
    override fun call(shouldFail: Boolean): String {
        if (shouldFail) throw RuntimeException("external API 5xx")
        return "OK"
    }
}

// Scenario impl: real HTTP call to a configurable base URL (WireMock in the
// docker scenario). shouldFail selects the endpoint; WireMock decides the
// response. retrieve() throws on 4xx/5xx, which propagates like a real failure.
@Component
@Profile("scenario")
class HttpExternalApiClient(
    builder: RestClient.Builder,
    @Value("\${external.api.base-url}") baseUrl: String,
) : ExternalApiClient {
    private val client = builder.baseUrl(baseUrl).build()

    override fun call(shouldFail: Boolean): String {
        val path = if (shouldFail) "/external/unstable" else "/external/ok"
        return client.get().uri(path).retrieve().body(String::class.java) ?: ""
    }
}

// ---------------------------------------------------------------------------
// Inner @Transactional bean. Calling this from another @Transactional method
// joins the SAME physical transaction (Propagation.REQUIRED). When it throws,
// Spring marks that physical transaction rollback-only -- even if the caller
// catches the exception.
// ---------------------------------------------------------------------------

@Service
class InnerApiTxService(
    private val repo: WorkLogRepository,
    private val api: ExternalApiClient,
) {
    @Transactional(propagation = Propagation.REQUIRED)
    fun callApiAndMarkSuccess(logId: Long, shouldFail: Boolean) {
        api.call(shouldFail) // throws here on failure -> tx becomes rollback-only
        val log = repo.findById(logId).orElseThrow()
        log.status = WorkStatus.SUCCESS
        log.detail = "api ok"
    }
}

// ---------------------------------------------------------------------------
// BROKEN: single @Transactional. We catch the exception and try to record
// FAILED, but the shared transaction is already rollback-only, so at commit
// time everything (including the FAILED write AND the initial PENDING insert)
// is rolled back and Spring throws UnexpectedRollbackException.
// ---------------------------------------------------------------------------

@Service
class BrokenService(
    private val repo: WorkLogRepository,
    private val inner: InnerApiTxService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun process(shouldFail: Boolean): Long {
        val entry = repo.save(WorkLog(status = WorkStatus.PENDING, detail = "started"))
        try {
            inner.callApiAndMarkSuccess(entry.id!!, shouldFail)
        } catch (e: Exception) {
            // Looks like we handled it -- but the physical tx is rollback-only.
            log.warn("caught {} -> trying to record FAILED (this write will be rolled back)", e.message)
            entry.status = WorkStatus.FAILED
            entry.detail = "failed: ${e.message}"
            repo.save(entry)
        }
        return entry.id!!
    }
}

// ---------------------------------------------------------------------------
// FIXED: no method-level @Transactional. TransactionTemplate gives explicit
// control of boundaries. The API call + success write run in tx #1; if that
// throws, tx #1 rolls back and we open a fresh tx #2 in the catch block to
// commit the FAILED record. The failure is durably recorded.
// ---------------------------------------------------------------------------

@Service
class FixedService(
    private val repo: WorkLogRepository,
    private val api: ExternalApiClient,
    private val txTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun process(shouldFail: Boolean): Long {
        return try {
            // tx #1: persist PENDING, call API, mark SUCCESS -- all or nothing.
            txTemplate.execute {
                val entry = repo.save(WorkLog(status = WorkStatus.PENDING, detail = "started"))
                api.call(shouldFail) // throws -> tx #1 rolls back, PENDING insert undone
                entry.status = WorkStatus.SUCCESS
                entry.detail = "api ok"
                repo.save(entry).id!!
            }!!
        } catch (e: Exception) {
            log.warn("caught {} -> recording FAILED in a new transaction", e.message)
            // tx #2: brand-new transaction, commits independently.
            txTemplate.execute {
                repo.save(WorkLog(status = WorkStatus.FAILED, detail = "failed: ${e.message}")).id!!
            }!!
        }
    }
}

// ---------------------------------------------------------------------------
// Facade composition: a @Transactional facade starts the transaction, then
// calls two sub-services -- one declarative (@Transactional), one programmatic
// (TransactionTemplate over the same PlatformTransactionManager). Both default
// to Propagation.REQUIRED, so both PARTICIPATE in the facade's single physical
// transaction: if the facade rolls back, the writes from BOTH sub-services
// roll back together.
// ---------------------------------------------------------------------------

// Sub-service #1: declarative @Transactional. REQUIRED joins the facade tx.
@Service
class TxAnnotatedSubService(private val repo: WorkLogRepository) {
    @Transactional
    fun save(tag: String): Long =
        repo.save(WorkLog(status = WorkStatus.SUCCESS, detail = "A:$tag")).id!!
}

// Sub-service #2: programmatic via TransactionTemplate (PlatformTransactionManager).
// Default propagation REQUIRED -> when called inside the facade's active tx it
// joins it (does NOT open a new physical tx). REQUIRES_NEW would isolate it.
@Service
class TxTemplateSubService(
    private val repo: WorkLogRepository,
    private val txTemplate: TransactionTemplate,
) {
    fun saveJoining(tag: String): Long =
        txTemplate.execute {
            repo.save(WorkLog(status = WorkStatus.SUCCESS, detail = "B:$tag")).id!!
        }!!

    fun saveRequiresNew(tag: String): Long {
        val newTx = TransactionTemplate(txTemplate.transactionManager!!).apply {
            propagationBehavior = org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }
        return newTx.execute {
            repo.save(WorkLog(status = WorkStatus.SUCCESS, detail = "B-NEW:$tag")).id!!
        }!!
    }
}

@Service
class FacadeService(
    private val annotated: TxAnnotatedSubService,
    private val template: TxTemplateSubService,
) {
    // Facade owns the transaction boundary. Both sub-services join it.
    @Transactional
    fun runJoining(fail: Boolean) {
        annotated.save("x")        // @Transactional REQUIRED -> joins
        template.saveJoining("y")  // TransactionTemplate REQUIRED -> joins
        if (fail) throw RuntimeException("facade boom")
    }

    // Same, but the template sub-service uses REQUIRES_NEW: its write commits
    // independently and survives the facade rollback.
    @Transactional
    fun runWithRequiresNew(fail: Boolean) {
        annotated.save("x")            // joins -> rolled back with facade
        template.saveRequiresNew("y")  // separate tx -> committed already
        if (fail) throw RuntimeException("facade boom")
    }
}

// ---------------------------------------------------------------------------
// HTTP surface to play with both services.
// ---------------------------------------------------------------------------

@RestController
class DemoController(
    private val broken: BrokenService,
    private val fixed: FixedService,
    private val repo: WorkLogRepository,
) {
    data class Result(val mode: String, val fail: Boolean, val savedId: Long?, val rows: List<RowView>, val error: String?)
    data class RowView(val id: Long?, val status: WorkStatus, val detail: String)

    @PostMapping("/broken")
    fun broken(@RequestParam(defaultValue = "true") fail: Boolean): Result = run("broken", fail) { broken.process(fail) }

    @PostMapping("/fixed")
    fun fixed(@RequestParam(defaultValue = "true") fail: Boolean): Result = run("fixed", fail) { fixed.process(fail) }

    @GetMapping("/logs")
    fun logs(): List<RowView> = repo.findAll().map { RowView(it.id, it.status, it.detail) }

    private fun run(mode: String, fail: Boolean, action: () -> Long): Result {
        var id: Long? = null
        var err: String? = null
        try {
            id = action()
        } catch (e: Exception) {
            err = "${e.javaClass.simpleName}: ${e.message}"
        }
        val rows = repo.findAll().map { RowView(it.id, it.status, it.detail) }
        return Result(mode, fail, id, rows, err)
    }
}
