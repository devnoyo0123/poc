package com.example.royalty.service

import com.example.royalty.dto.ExcelUploadDetailResponse
import com.example.royalty.dto.ExcelUploadResponse
import com.example.royalty.dto.RoyaltyRecordResponse
import com.example.royalty.entity.ExcelUpload
import com.example.royalty.entity.RoyaltyRecord
import com.example.royalty.repository.ExcelTypeMappingRepository
import com.example.royalty.repository.ExcelUploadRepository
import com.example.royalty.repository.RoyaltyRecordRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.UUID

@Service
class ExcelUploadService(
    private val excelUploadRepository: ExcelUploadRepository,
    private val excelTypeMappingRepository: ExcelTypeMappingRepository,
    private val royaltyRecordRepository: RoyaltyRecordRepository,
    private val s3Client: S3Client,
    private val excelParsingService: ExcelParsingService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun uploadExcel(type: String, file: MultipartFile): ExcelUploadResponse {
        log.info("엑셀 업로드 시작: type=$type, filename=${file.originalFilename}")

        // 타입 매핑 조회
        val typeMapping = excelTypeMappingRepository.findByType(type)
            ?: throw IllegalArgumentException("알 수 없는 타입입니다: $type")

        // 업로드 이력 생성
        val upload = ExcelUpload(
            type = type,
            originalFilename = file.originalFilename ?: "unknown.xlsx"
        )
        val savedUpload = excelUploadRepository.save(upload)

        // S3에 원본 파일 업로드
        val s3Key = "excel/${savedUpload.id}/${file.originalFilename}"
        uploadToS3(s3Key, file.bytes)

        savedUpload.s3Key = s3Key
        savedUpload.s3Bucket = "royalty-excel-uploads"

        // 엑셀 파싱 및 DB 저장
        val parsedUpload = excelParsingService.parseAndSave(file, type, typeMapping, savedUpload)

        return toResponse(parsedUpload, typeMapping)
    }

    @Transactional(readOnly = true)
    fun getUploadDetail(uploadId: UUID): ExcelUploadDetailResponse {
        val upload = excelUploadRepository.findById(uploadId).orElseThrow {
            IllegalArgumentException("엑로드 이력을 찾을 수 없습니다: $uploadId")
        }

        val records = royaltyRecordRepository.findByUploadId(uploadId)

        return ExcelUploadDetailResponse(
            uploadId = upload.id.toString(),
            type = upload.type,
            originalFilename = upload.originalFilename,
            s3Key = upload.s3Key,
            status = upload.status,
            totalRows = upload.totalRows,
            processedRows = upload.processedRows,
            errorMessage = upload.errorMessage,
            createdAt = upload.createdAt.toString(),
            records = records.map { toRecordResponse(it) }
        )
    }

    private fun uploadToS3(key: String, content: ByteArray) {
        val request = PutObjectRequest.builder()
            .bucket("royalty-excel-uploads")
            .key(key)
            .build()

        s3Client.putObject(request, RequestBody.fromBytes(content))
        log.info("S3 업로드 완료: $key")
    }

    private fun toResponse(upload: ExcelUpload, typeMapping: com.example.royalty.entity.ExcelTypeMapping): ExcelUploadResponse {
        return ExcelUploadResponse(
            uploadId = upload.id.toString(),
            type = upload.type,
            typeName = typeMapping.typeName,
            originalFilename = upload.originalFilename,
            status = upload.status,
            totalRows = upload.totalRows,
            processedRows = upload.processedRows,
            headerMapping = typeMapping.columnMappings
        )
    }

    private fun toRecordResponse(record: RoyaltyRecord): RoyaltyRecordResponse {
        return RoyaltyRecordResponse(
            id = record.id!!,
            rowNumber = record.rowNumber,
            originalData = record.originalData,
            matchedData = record.matchedData
        )
    }
}
