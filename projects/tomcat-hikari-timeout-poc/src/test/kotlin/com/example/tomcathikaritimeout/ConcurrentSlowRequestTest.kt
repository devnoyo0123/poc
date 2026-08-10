package com.example.tomcathikaritimeout

import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.wait.strategy.Wait

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrentSlowRequestTest : StringSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    init {
        "5 concurrent slow requests with pool=2 produce 2 success and 3 HikariCP timeouts" {
            val concurrency = 5
            val sleepSeconds = 5L
            val executor = Executors.newFixedThreadPool(concurrency)

            log.info("[TEST] firing {} concurrent POSTs sleepSeconds={} ts={}", concurrency, sleepSeconds, Instant.now())

            val futures = (1..concurrency).map { idx ->
                CompletableFuture.supplyAsync({
                    log.info("[TEST] submit idx={} runnerThread={}", idx, Thread.currentThread().name)
                    postSlow(sleepSeconds)
                }, executor)
            }

            val responses = CompletableFuture.allOf(*futures.toTypedArray())
                .get(20, TimeUnit.SECONDS)
                .let { futures.map { it.get() } }

            executor.shutdown()

            log.info("[TEST] collected {} responses ts={}", responses.size, Instant.now())
            responses.forEachIndexed { idx, response ->
                log.info("[TEST] idx={} status={} body={}", idx, response.statusCode, response.body)
            }

            responses shouldHaveSize concurrency

            val okResponses = responses.filter { it.statusCode == HttpStatus.OK }
            val timeoutResponses = responses.filter { it.statusCode == HttpStatus.SERVICE_UNAVAILABLE }

            okResponses shouldHaveSize 2
            timeoutResponses shouldHaveSize 3

            okResponses.forEach { response ->
                val body = response.body!!
                body["status"] shouldBe "ok"
                (body["workerThread"] as String) shouldContain "http-nio-"
            }

            timeoutResponses.forEach { response ->
                val body = response.body!!
                val message = body["message"] as String
                message shouldContain "Connection is not available, request timed out after"
            }
        }
    }

    private fun postSlow(sleepSeconds: Long): ResponseEntity<Map<*, *>> =
        restTemplate.postForEntity(
            "/api/v1/slow?sleepSeconds=$sleepSeconds",
            null,
            Map::class.java,
        )

    companion object {
        private val log = LoggerFactory.getLogger(ConcurrentSlowRequestTest::class.java)

        private const val POSTGRES_SERVICE = "postgres-1"
        private const val POSTGRES_PORT = 5432

        private val compose = ComposeContainer(File("docker-compose.yml")).apply {
            withExposedService(POSTGRES_SERVICE, POSTGRES_PORT, Wait.forListeningPort())
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerPostgresProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") {
                val host = compose.getServiceHost(POSTGRES_SERVICE, POSTGRES_PORT)
                val port = compose.getServicePort(POSTGRES_SERVICE, POSTGRES_PORT)
                "jdbc:postgresql://$host:$port/tomcat_hikari_timeout"
            }
            registry.add("spring.datasource.username") { "poc" }
            registry.add("spring.datasource.password") { "poc" }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
        }
    }
}
