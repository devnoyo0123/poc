package com.example.s3sqs.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.UUID

@Service
class S3UploadService(
    private val s3Client: S3Client,
    @Value("\${app.aws.s3.bucket-name}") private val bucketName: String
) {

    fun upload(fileName: String, content: ByteArray): String {
        val key = "${UUID.randomUUID()}/$fileName"

        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build()

        s3Client.putObject(request, RequestBody.fromBytes(content))

        return key
    }
}
