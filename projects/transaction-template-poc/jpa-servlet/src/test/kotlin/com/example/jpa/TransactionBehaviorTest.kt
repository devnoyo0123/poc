package com.example.jpa

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class TransactionBehaviorTest(
    @Autowired val broken: BrokenService,
    @Autowired val fixed: FixedService,
    @Autowired val repo: WorkLogRepository,
) {
    @BeforeEach
    fun clean() = repo.deleteAll()

    @Test
    fun `broken - on API failure the FAILED record is lost to rollback`() {
        // Catch inside the @Transactional method does not save us: the shared
        // transaction is rollback-only, so commit throws and everything rolls back.
        val thrown = catchThrowable { broken.process(shouldFail = true) }

        // UnexpectedRollbackException is itself the proof that the inner @Transactional
        // call joined the SAME physical tx and marked it rollback-only: a separate tx
        // would have let the outer commit the FAILED write instead of refusing to commit.
        assertThat(thrown).isInstanceOf(org.springframework.transaction.UnexpectedRollbackException::class.java)
        assertThat(repo.findAll())
            .describedAs("no FAILED row survived -- the whole tx rolled back")
            .isEmpty()
    }

    @Test
    fun `broken - happy path commits SUCCESS`() {
        val id = broken.process(shouldFail = false)
        assertThat(repo.findById(id).get().status).isEqualTo(WorkStatus.SUCCESS)
    }

    @Test
    fun `fixed - on API failure the FAILED record is committed in a new tx`() {
        val id = fixed.process(shouldFail = true)

        val rows = repo.findAll()
        assertThat(rows).hasSize(1)
        assertThat(rows[0].id).isEqualTo(id)
        assertThat(rows[0].status)
            .describedAs("FAILED durably recorded via second TransactionTemplate tx")
            .isEqualTo(WorkStatus.FAILED)
        // The PENDING insert from tx #1 was rolled back, so only the FAILED row exists.
    }

    @Test
    fun `fixed - happy path commits SUCCESS`() {
        val id = fixed.process(shouldFail = false)
        assertThat(repo.findById(id).get().status).isEqualTo(WorkStatus.SUCCESS)
    }
}
