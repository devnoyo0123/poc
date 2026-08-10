package com.example.jpa

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * The real question: a @Transactional facade starts the transaction, then calls
 * two sub-services -- one declarative (@Transactional), one programmatic
 * (TransactionTemplate over the SAME PlatformTransactionManager). Do they share
 * the facade's transaction?
 */
@SpringBootTest
class FacadeCompositionTest(
    @Autowired val facade: FacadeService,
    @Autowired val repo: WorkLogRepository,
) {
    @BeforeEach
    fun clean() = repo.deleteAll()

    @Test
    fun `happy path - both sub-services commit with the facade`() {
        facade.runJoining(fail = false)
        assertThat(repo.findAll().map { it.detail })
            .containsExactlyInAnyOrder("A:x", "B:y")
    }

    @Test
    fun `facade rollback - TransactionTemplate sub-service joined the facade tx and rolls back too`() {
        val thrown = catchThrowable { facade.runJoining(fail = true) }

        assertThat(thrown).isInstanceOf(RuntimeException::class.java)
        assertThat(repo.findAll())
            .describedAs("both the @Transactional AND the TransactionTemplate writes rolled back -> same physical tx")
            .isEmpty()
    }

    @Test
    fun `REQUIRES_NEW - TransactionTemplate sub-service commits independently and survives facade rollback`() {
        val thrown = catchThrowable { facade.runWithRequiresNew(fail = true) }

        assertThat(thrown).isInstanceOf(RuntimeException::class.java)
        val rows = repo.findAll()
        assertThat(rows).hasSize(1)
        assertThat(rows[0].detail)
            .describedAs("only the REQUIRES_NEW write survived; the facade-joined write rolled back")
            .isEqualTo("B-NEW:y")
    }
}
