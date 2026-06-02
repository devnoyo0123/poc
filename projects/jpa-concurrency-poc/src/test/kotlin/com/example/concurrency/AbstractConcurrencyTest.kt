package com.example.concurrency

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop"
    ]
)
@Testcontainers
abstract class AbstractConcurrencyTest {

    companion object {
        @Container
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(
            DockerImageName.parse("postgres:16-alpine")
        ).withDatabaseName("concurrency_db")
            .withUsername("test_user")
            .withPassword("test_pass")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") {
                "jdbc:postgresql://${postgres.host}:${postgres.firstMappedPort}/concurrency_db"
            }
            registry.add("spring.datasource.username") { "test_user" }
            registry.add("spring.datasource.password") { "test_pass" }
            registry.add("spring.jpa.properties.hibernate.dialect") {
                "org.hibernate.dialect.PostgreSQLDialect"
            }
        }
    }
}
