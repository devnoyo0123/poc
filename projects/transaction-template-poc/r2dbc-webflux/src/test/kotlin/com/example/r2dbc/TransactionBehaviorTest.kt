package com.example.r2dbc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.test.StepVerifier

@SpringBootTest
class TransactionBehaviorTest(
    @Autowired val broken: BrokenService,
    @Autowired val fixed: FixedService,
    @Autowired val repo: WorkLogRepository,
) {
    @BeforeEach
    fun clean() {
        repo.deleteAll().block()
    }

    @Test
    fun `broken - on API failure the FAILED record is lost to rollback`() {
        StepVerifier.create(broken.process(shouldFail = true))
            .expectError(RuntimeException::class.java)
            .verify()

        // Everything inside the single tx rolled back -- no row at all.
        StepVerifier.create(repo.count())
            .expectNext(0L)
            .verifyComplete()
    }

    @Test
    fun `broken - happy path commits SUCCESS`() {
        val id = broken.process(shouldFail = false).block()!!
        assertThat(repo.findById(id).block()!!.status).isEqualTo(WorkStatus.SUCCESS)
    }

    @Test
    fun `fixed - on API failure the FAILED record is committed in a new tx`() {
        val id = fixed.process(shouldFail = true).block()!!

        val rows = repo.findAll().collectList().block()!!
        assertThat(rows).hasSize(1)
        assertThat(rows[0].id).isEqualTo(id)
        assertThat(rows[0].status)
            .describedAs("FAILED durably recorded via second TransactionalOperator tx")
            .isEqualTo(WorkStatus.FAILED)
    }

    @Test
    fun `fixed - happy path commits SUCCESS`() {
        val id = fixed.process(shouldFail = false).block()!!
        assertThat(repo.findById(id).block()!!.status).isEqualTo(WorkStatus.SUCCESS)
    }
}
