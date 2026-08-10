package com.example.asyncrejection

import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import java.io.File
import java.util.concurrent.TimeUnit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.wait.strategy.Wait

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AsyncRejectionFlowTest : StringSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var exportService: ExportService

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    init {
        "postgres testcontainer is wired into spring context" {
            jdbcTemplate.queryForObject("select 1", Int::class.java) shouldBe 1
        }

        "rejected async submit is handled on original http request thread" {
            exportService.resetProbe()

            val first = postExport(2500)

            first.statusCode shouldBe HttpStatus.ACCEPTED
            first.body!!["message"] shouldBe "submit succeeded"
            exportService.awaitStarted(2, TimeUnit.SECONDS) shouldBe true
            exportService.lastWorkerThread() shouldStartWith "excel-export-"

            val rejected = postExport(100)

            rejected.statusCode shouldBe HttpStatus.TOO_MANY_REQUESTS
            rejected.body!!["exception"] shouldBe "TaskRejectedException"
            rejected.body!!["message"] shouldBe "submit rejected before async handoff"
            rejected.body!!["requestThread"] as String shouldStartWith "http-nio-"
        }
    }

    private fun postExport(sleepMillis: Long): ResponseEntity<Map<*, *>> =
        restTemplate.postForEntity(
            "/api/v1/half-yearly-distributions/excel?sleepMillis=$sleepMillis",
            null,
            Map::class.java,
        )

    companion object {
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
                "jdbc:postgresql://$host:$port/async_rejection"
            }
            registry.add("spring.datasource.username") { "poc" }
            registry.add("spring.datasource.password") { "poc" }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
        }
    }
}
