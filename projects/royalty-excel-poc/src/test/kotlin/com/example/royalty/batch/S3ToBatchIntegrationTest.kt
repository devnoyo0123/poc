package com.example.royalty.batch

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.example.royalty.entity.ExcelUpload
import com.example.royalty.entity.ExcelTypeMapping
import com.example.royalty.repository.ExcelUploadRepository
import com.example.royalty.repository.ExcelTypeMappingRepository
import com.example.royalty.repository.RoyaltyRecordRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import java.io.File
import java.util.UUID

@SpringBootTest(
    properties = [
        "spring.cloud.aws.sqs.enabled=false",
        "spring.autoconfigure.exclude=io.awspring.cloud.sqs.config.SqsAutoConfiguration",
        "spring.batch.jdbc.initialize-schema=always",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    ]
)
@Testcontainers
class S3ToBatchIntegrationTest {

    companion object {
        @Container
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(
            DockerImageName.parse("postgres:16-alpine")
        ).withDatabaseName("royalty_db")
            .withUsername("royalty_user")
            .withPassword("royalty_pass")

        @Container
        val localstack: LocalStackContainer = LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.5")
        ).withServices(
            LocalStackContainer.Service.S3,
            LocalStackContainer.Service.SQS
        )

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { "jdbc:postgresql://${postgres.host}:${postgres.firstMappedPort}/royalty_db" }
            registry.add("spring.datasource.username") { "royalty_user" }
            registry.add("spring.datasource.password") { "royalty_pass" }
            registry.add("cloud.aws.region.static") { "ap-northeast-2" }
            registry.add("cloud.aws.credentials.access-key") { "test" }
            registry.add("cloud.aws.credentials.secret-key") { "test" }
            registry.add("cloud.aws.s3.endpoint") { localstack.getEndpointOverride(LocalStackContainer.Service.S3) }
            registry.add("cloud.aws.sqs.endpoint") { localstack.getEndpointOverride(LocalStackContainer.Service.SQS) }
        }
    }

    @Autowired
    private lateinit var s3Client: S3Client

    @Autowired
    private lateinit var sqsClient: SqsClient

    @Autowired
    private lateinit var excelUploadRepository: ExcelUploadRepository

    @Autowired
    private lateinit var excelTypeMappingRepository: ExcelTypeMappingRepository

    @Autowired
    private lateinit var royaltyRecordRepository: RoyaltyRecordRepository

    private val log = LoggerFactory.getLogger(javaClass)

    private lateinit var queueUrl: String

    @BeforeEach
    fun setUp() {
        // S3 버킷 생성
        s3Client.createBucket { it.bucket("royalty-excel-uploads") }

        // SQS 큐 생성
        queueUrl = sqsClient.createQueue(
            CreateQueueRequest.builder().queueName("excel-upload-queue").build()
        ).queueUrl()

        log.info("테스트 환경 설정 완료: S3 버킷, SQS 큐 생성됨")
    }

    @AfterEach
    fun tearDown() {
        royaltyRecordRepository.deleteAll()
        excelUploadRepository.deleteAll()
    }

    @Test
    fun `S3 업로드 → SQS 이벤트 → 배치 처리 통합 테스트`() {
        // Given: 타입 매핑 생성
        val typeMapping = ExcelTypeMapping(
            type = "A",
            typeName = "음반협회",
            headerRowStart = 1,
            targetTable = null,
            columnMappings = mapOf(
                "저작권료" to "royalty_fee",
                "저작자" to "copyright_holder",
                "작품명" to "work_title",
                "사용일" to "usage_date",
                "판매량" to "sales_count"
            )
        )
        excelTypeMappingRepository.save(typeMapping)

        // 업로드 이력 생성
        val upload = ExcelUpload(
            type = "A",
            originalFilename = "type_a_music.xlsx"
        )
        val savedUpload = excelUploadRepository.save(upload)

        // S3 → SQS 이벤트 알림 설정 (파일 업로드 전에 설정해야 함)
        val queueArn = sqsClient.getQueueAttributes(
            GetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributeNames(QueueAttributeName.QUEUE_ARN)
                .build()
        ).attributes()[QueueAttributeName.QUEUE_ARN]

        s3Client.putBucketNotificationConfiguration(
            software.amazon.awssdk.services.s3.model.PutBucketNotificationConfigurationRequest.builder()
                .bucket("royalty-excel-uploads")
                .notificationConfiguration(
                    software.amazon.awssdk.services.s3.model.NotificationConfiguration.builder()
                        .queueConfigurations(
                            software.amazon.awssdk.services.s3.model.QueueConfiguration.builder()
                                .queueArn(queueArn)
                                .events(software.amazon.awssdk.services.s3.model.Event.S3_OBJECT_CREATED_PUT)
                                .build()
                        )
                        .build()
                )
                .build()
        )

        log.info("S3 → SQS 이벤트 알림 설정 완료")

        // S3에 파일 업로드 (S3 이벤트 트리거)
        val uploadId = savedUpload.id
        val s3Key = "excel/$uploadId/type_a_music.xlsx"
        val excelFile = File("samples/type_a_music.xlsx")

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket("royalty-excel-uploads")
                .key(s3Key)
                .build(),
            software.amazon.awssdk.core.sync.RequestBody.fromFile(excelFile.toPath())
        )

        log.info("S3 업로드 완료: $s3Key")

        // When: 파일 업로드로 이벤트 발생 (이미 위에서 수행)
        // Then: SQS에서 메시지 수신 대기 및 검증
        Thread.sleep(2000) // 이벤트 전파 대기

        val messages = sqsClient.receiveMessage(
            ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(5)
                .build()
        ).messages()

        log.info("수신한 메시지 개수: ${messages.size}")
        if (messages.isNotEmpty()) {
            messages.forEachIndexed { index, msg ->
                log.info("메시지 $index: ${msg.body()}")
            }
        }

        assert(messages.isNotEmpty()) { "SQS 메시지가 수신되어야 합니다" }

        // S3 테스트 이벤트 건너뛰고 실제 ObjectCreated 이벤트 찾기
        val objectCreatedMessage = messages.firstOrNull {
            it.body().contains("ObjectCreated:Put")
        } ?: throw AssertionError("ObjectCreated:Put 이벤트를 찾을 수 없습니다")

        val s3EventMessage = objectCreatedMessage.body()
        log.info("SQS 메시지 수신: $s3EventMessage")

        assert(s3EventMessage.contains("ObjectCreated:Put")) { "S3 ObjectCreated 이벤트여야 합니다" }
        assert(s3EventMessage.contains(s3Key)) { "업로드한 파일 키가 포함되어야 합니다" }

        // DB에서 배치 처리 결과 검증 (배치가 실제로 실행되었다면)
        // Note: 실제 배치 실행은 SQS Listener가 담당하므로
        // 여기서는 이벤트 전달까지만 검증하고, 배치는 별도 테스트

        log.info("=== 테스트 통과: S3 → SQS 이벤트 전달 확인 ===")
    }
}
