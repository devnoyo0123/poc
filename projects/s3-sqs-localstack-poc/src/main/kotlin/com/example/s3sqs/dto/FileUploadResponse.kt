package com.example.s3sqs.dto

data class FileUploadResponse(
    val key: String,
    val bucket: String,
    val size: Long,
    val message: String
)
