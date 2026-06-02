package com.example.royalty.batch

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

@Component
class S3EventListener(
    private val jobLauncher: JobLauncher,
    private val processExcelFileJob: Job,
    private val sqsClient: SqsClient,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val QUEUE_URL = "http://localhost:4566/000000000000/excel-upload-queue"
    }

    @SqsListener("\${app.sqs.queue-name:excel-upload-queue}")
    fun handleS3Event(message: String) {
        log.info("=== SQS Message Received ===")
        log.info("Raw message: {}", message)

        try {
            val jsonNode = objectMapper.readTree(message)

            // S3 이벤트 레코드 파싱
            val records = jsonNode.path("Records")
            if (records.isArray && records.size() > 0) {
                val record = records[0]
                val eventName = record.path("eventName").asText()

                // ObjectCreated:* 이벤트만 처리
                if (eventName.startsWith("ObjectCreated:")) {
                    val s3Info = record.path("s3")
                    val bucket = s3Info.path("bucket").path("name").asText()
                    val key = s3Info.path("object").path("key").asText()

                    log.info("S3 ObjectCreated 이벤트: bucket=$bucket, key=$key")

                    // S3 키에서 타입과 uploadId 추출
                    // 예: excel/{uploadId}/{filename} → uploadId, type은 파라미터나 메타데이터에서
                    val keyParts = key.split("/")
                    if (keyParts.size >= 2 && keyParts[0] == "excel") {
                        val uploadId = keyParts[1]

                        // 일단 타입 A로 테스트 (실제로는 메타데이터나 별도 조회 필요)
                        val type = "A"

                        log.info("배치 잡 실행 시작: uploadId=$uploadId, type=$type")

                        // 비동기 배치 잡 실행
                        val jobParameters = JobParametersBuilder()
                            .addString("s3Key", key)
                            .addString("type", type)
                            .addString("uploadId", uploadId)
                            .addLong("timestamp", System.currentTimeMillis())
                            .toJobParameters()

                        val jobExecution = jobLauncher.run(processExcelFileJob, jobParameters)

                        log.info("배치 잡 완료: jobId=${jobExecution.jobId}, status=${jobExecution.status}")
                    }
                }
            }

            log.info("=== Message Processing Complete ===")

        } catch (e: Exception) {
            log.error("메시지 처리 실패", e)
            throw e
        }
    }
}
