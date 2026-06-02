package com.example.royalty.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BatchJobLauncher(
    private val jobLauncher: JobLauncher,
    private val jobRepository: JobRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun launchJob(
        job: Job,
        s3Key: String,
        type: String,
        uploadId: UUID
    ) {
        try {
            val jobParameters = JobParametersBuilder()
                .addString("s3Key", s3Key)
                .addString("type", type)
                .addString("uploadId", uploadId.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters()

            val jobExecution = jobLauncher.run(job, jobParameters)

            log.info("배치 잡 완료: jobId=${jobExecution.jobId}, status=${jobExecution.status}")

        } catch (e: Exception) {
            log.error("배치 잡 실행 실패", e)
            throw e
        }
    }
}
