package com.example.batch.config

import com.example.batch.tasklet.ParameterDemoTasklet
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.support.transaction.ResourcelessTransactionManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ParameterDemoJobConfig(
    private val jobRepository: JobRepository,
    private val parameterDemoTasklet: ParameterDemoTasklet
) {

    @Bean
    fun parameterDemoJob(): Job {
        return JobBuilder("parameterDemoJob", jobRepository)
            .start(parameterDemoStep())
            .build()
    }

    @Bean
    fun parameterDemoStep(): Step {
        val transactionManager = ResourcelessTransactionManager()

        return StepBuilder("parameterDemoStep", jobRepository)
            .tasklet(parameterDemoTasklet, transactionManager)
            .build()
    }
}
