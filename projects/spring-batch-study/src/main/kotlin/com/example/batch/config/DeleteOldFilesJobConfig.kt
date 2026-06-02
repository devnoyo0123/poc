package com.example.batch.config

import com.example.batch.tasklet.DeleteOldFilesTasklet
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.support.transaction.ResourcelessTransactionManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class DeleteOldFilesJobConfig(
    private val jobRepository: JobRepository,
    private val deleteOldFilesTasklet: DeleteOldFilesTasklet
) {

    @Bean
    fun deleteOldFilesJob(): Job {
        return JobBuilder("deleteOldFilesJob", jobRepository)
            .start(deleteOldFilesStep())
            .build()
    }

    @Bean
    fun deleteOldFilesStep(): Step {
        // ResourcelessTransactionManager 사용 (DB 연동 없는 작업)
        val transactionManager = ResourcelessTransactionManager()

        return StepBuilder("deleteOldFilesStep", jobRepository)
            .tasklet(deleteOldFilesTasklet, transactionManager)
            .build()
    }
}
