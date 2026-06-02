package com.example.s3sqs.controller

import com.example.s3sqs.dto.FileUploadResponse
import com.example.s3sqs.service.S3UploadService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/files")
class FileUploadController(
    private val s3UploadService: S3UploadService,
    @Value("\${app.aws.s3.bucket-name}") private val bucketName: String
) {

    @PostMapping("/upload")
    fun uploadFile(@RequestParam file: MultipartFile): ResponseEntity<FileUploadResponse> {
        val key = s3UploadService.upload(
            fileName = file.originalFilename ?: "unknown",
            content = file.bytes
        )

        return ResponseEntity.ok(
            FileUploadResponse(
                key = key,
                bucket = bucketName,
                size = file.size,
                message = "File uploaded. S3 event notification will be sent to SQS."
            )
        )
    }
}
