package com.example.s3sqs.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.sqs.SqsClient
import java.net.URI

@Configuration
class AwsConfig {

    @Value("\${cloud.aws.region.static:ap-northeast-2}")
    private lateinit var region: String

    @Value("\${cloud.aws.credentials.access-key:test}")
    private lateinit var accessKey: String

    @Value("\${cloud.aws.credentials.secret-key:test}")
    private lateinit var secretKey: String

    @Value("\${cloud.aws.s3.endpoint:}")
    private lateinit var s3Endpoint: String

    @Value("\${cloud.aws.sqs.endpoint:}")
    private lateinit var sqsEndpoint: String

    @Bean
    fun credentialsProvider(): StaticCredentialsProvider {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey)
        )
    }

    @Bean
    fun s3Client(): S3Client {
        val builder = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider())

        if (s3Endpoint.isNotBlank()) {
            builder.endpointOverride(URI.create(s3Endpoint))
                .forcePathStyle(true) // LocalStack requires path-style
        }

        return builder.build()
    }

    @Bean
    fun sqsClient(): SqsClient {
        val builder = SqsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider())

        if (sqsEndpoint.isNotBlank()) {
            builder.endpointOverride(URI.create(sqsEndpoint))
        }

        return builder.build()
    }
}
