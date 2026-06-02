package com.example.s3sqs

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.Event
import software.amazon.awssdk.services.s3.model.NotificationConfiguration
import software.amazon.awssdk.services.s3.model.PutBucketNotificationConfigurationRequest
import software.amazon.awssdk.services.s3.model.QueueConfiguration
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3SqsIntegrationTest {

    companion object {
        @Container
        val localstack: LocalStackContainer = LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.5")
        ).withServices(
            LocalStackContainer.Service.S3,
            LocalStackContainer.Service.SQS
        )

        private const val BUCKET_NAME = "test-bucket"
        private const val QUEUE_NAME = "test-queue"
    }

    private lateinit var s3: S3Client
    private lateinit var sqs: SqsClient
    private lateinit var queueUrl: String

    @BeforeAll
    fun setUp() {
        val credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("test", "test")
        )
        val region = Region.AP_NORTHEAST_2

        s3 = S3Client.builder()
            .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
            .region(region)
            .credentialsProvider(credentialsProvider)
            .forcePathStyle(true)
            .build()

        sqs = SqsClient.builder()
            .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SQS))
            .region(region)
            .credentialsProvider(credentialsProvider)
            .build()

        // 버킷 생성
        s3.createBucket { it.bucket(BUCKET_NAME) }

        // 큐 생성
        queueUrl = sqs.createQueue(CreateQueueRequest.builder().queueName(QUEUE_NAME).build()).queueUrl()

        // 큐 ARN 조회
        val queueArn = sqs.getQueueAttributes(
            GetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributeNames(QueueAttributeName.QUEUE_ARN)
                .build()
        ).attributes()[QueueAttributeName.QUEUE_ARN]

        // S3 → SQS 이벤트 알림 설정
        val notificationRequest = PutBucketNotificationConfigurationRequest.builder()
            .bucket(BUCKET_NAME)
            .notificationConfiguration(
                NotificationConfiguration.builder()
                    .queueConfigurations(
                        QueueConfiguration.builder()
                            .queueArn(queueArn)
                            .events(Event.S3_OBJECT_CREATED_PUT)
                            .build()
                    )
                    .build()
            )
            .build()
        s3.putBucketNotificationConfiguration(notificationRequest)

        // LocalStack이 알림 설정 시 보내는 TestEvent 메시지 제거
        purgeTestEvents()
    }

    private fun purgeTestEvents() {
        var messages = sqs.receiveMessage(
            ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(2)
                .build()
        ).messages()
        // 큐가 빌 때까지 계속 제거
        while (messages.isNotEmpty()) {
            messages = sqs.receiveMessage(
                ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(1)
                    .build()
            ).messages()
        }
    }

    @Test
    fun `S3 upload triggers SQS event notification`() {
        // S3에 파일 업로드
        s3.putObject(
            { it.bucket(BUCKET_NAME).key("test-file.txt") },
            software.amazon.awssdk.core.sync.RequestBody.fromString("hello localstack!")
        )

        // SQS에서 메시지 수신
        val messages = sqs.receiveMessage(
            ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(1)
                .waitTimeSeconds(5)
                .build()
        ).messages()

        assert(messages.isNotEmpty()) { "SQS message should be received after S3 upload" }

        val body = messages[0].body()
        assert(body.contains("ObjectCreated:Put")) { "Event should be ObjectCreated:Put, but was: $body" }
        assert(body.contains("test-file.txt")) { "Event should contain the uploaded key" }

        println("=== Test Passed ===")
        println("SQS Message received: $body")
    }

    @Test
    fun `Multiple file uploads trigger multiple SQS messages`() {
        // 여러 파일 업로드
        val files = listOf("file1.txt", "file2.txt", "file3.txt")
        files.forEach { fileName ->
            s3.putObject(
                { it.bucket(BUCKET_NAME).key(fileName) },
                software.amazon.awssdk.core.sync.RequestBody.fromString("content of $fileName")
            )
        }

        // SQS에서 메시지 수신
        val messages = sqs.receiveMessage(
            ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(5)
                .build()
        ).messages()

        assert(messages.size == 3) { "Should receive 3 messages, but got ${messages.size}" }

        val receivedKeys = messages.map { msg ->
            val body = msg.body()
            println("Message: $body")
            // 이벤트에서 key 추출 확인
            files.any { body.contains(it) }
        }
        assert(receivedKeys.all { it }) { "All uploaded files should be in SQS messages" }

        println("=== Multi-file Test Passed ===")
        println("Received ${messages.size} messages for ${files.size} uploads")
    }
}
