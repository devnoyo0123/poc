package com.example.batch.config

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Configuration

@Configuration
class BatchJobLauncher : CommandLineRunner {

    @Autowired
    private lateinit var jobLauncher: JobLauncher

    @Autowired
    private lateinit var deleteOldFilesJob: Job

    @Autowired
    private lateinit var parameterDemoJob: Job

    @Value("\${spring.batch.job.name:deleteOldFilesJob}")
    private lateinit var jobName: String

    override fun run(vararg args: String) {
        val jobParameters = JobParametersBuilder()
            .addString("basePath", System.getProperty("basePath") ?: "/tmp/spring-batch-logs")
            .addLong("daysOld", System.getProperty("daysOld")?.toLong() ?: 7L)
            .toJobParameters()

        val job = when (jobName) {
            "deleteOldFilesJob" -> deleteOldFilesJob
            "parameterDemoJob" -> parameterDemoJob
            else -> deleteOldFilesJob
        }

        println("🚀 Launching job: ${job.name}")
        jobLauncher.run(job, jobParameters)
    }
}
