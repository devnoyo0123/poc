package com.example.royalty.dto

data class ExcelUploadRequest(
    val type: String
)

data class ExcelUploadResponse(
    val uploadId: String,
    val type: String,
    val typeName: String,
    val originalFilename: String,
    val status: String,
    val totalRows: Int?,
    val processedRows: Int,
    val headerMapping: Map<String, String>?
)

data class ExcelUploadDetailResponse(
    val uploadId: String,
    val type: String,
    val originalFilename: String,
    val s3Key: String?,
    val status: String,
    val totalRows: Int?,
    val processedRows: Int,
    val errorMessage: String?,
    val createdAt: String,
    val records: List<RoyaltyRecordResponse>
)

data class RoyaltyRecordResponse(
    val id: Long,
    val rowNumber: Int,
    val originalData: Map<String, Any?>,
    val matchedData: Map<String, Any?>
)
