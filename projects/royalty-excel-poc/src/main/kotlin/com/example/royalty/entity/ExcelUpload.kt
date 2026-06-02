package com.example.royalty.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "excel_uploads")
class ExcelUpload(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val type: String,

    @Column(name = "original_filename", nullable = false)
    val originalFilename: String,

    @Column(name = "s3_key")
    var s3Key: String? = null,

    @Column(name = "s3_bucket")
    var s3Bucket: String? = null,

    @Column(nullable = false)
    var status: String = "PROCESSING",

    @Column(name = "total_rows")
    var totalRows: Int? = null,

    @Column(name = "processed_rows")
    var processedRows: Int = 0,

    @Column(name = "error_message")
    var errorMessage: String? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
