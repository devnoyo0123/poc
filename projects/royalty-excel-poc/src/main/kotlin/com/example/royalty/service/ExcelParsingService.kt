package com.example.royalty.service

import com.alibaba.excel.EasyExcel
import com.example.royalty.entity.ExcelTypeMapping
import com.example.royalty.entity.ExcelUpload
import com.example.royalty.entity.RoyaltyRecord
import com.example.royalty.excel.listener.RoyaltyExcelListener
import com.example.royalty.repository.ExcelUploadRepository
import com.example.royalty.repository.RoyaltyRecordRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.UUID

@Service
class ExcelParsingService(
    private val excelUploadRepository: ExcelUploadRepository,
    private val royaltyRecordRepository: RoyaltyRecordRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun parseAndSave(
        file: MultipartFile,
        type: String,
        typeMapping: ExcelTypeMapping,
        upload: ExcelUpload
    ): ExcelUpload {
        // 임시 파일 저장
        val tempFile = createTempFile(file)
        try {
            val rowCount = intArrayOf(0)

            // EasyExcel로 파싱 (SAX 스트리밍)
            val listener = RoyaltyExcelListener(
                typeMapping = typeMapping,
                headerRowStart = typeMapping.headerRowStart,
                sheetName = "Sheet1",  // 기본 시트 이름
                onDataRead = { rowNumber, rowData, headers ->
                    // 헤더 매칭 및 데이터 변환
                    val matchedData = matchHeaders(rowData, headers, typeMapping.columnMappings)

                    // 원본 데이터 (컬럼 인덱스 → 헤더명, 값)
                    val originalData = rowData.mapKeys { (colIndex, _) ->
                        headers[colIndex] ?: "col_$colIndex"
                    }

                    // DB 저장
                    val record = RoyaltyRecord(
                        uploadId = upload.id,
                        sheetName = "Sheet1",  // 기본 시트 이름
                        rowNumber = rowNumber,
                        originalData = originalData,
                        matchedData = matchedData,
                        headerMapping = typeMapping.columnMappings
                    )

                    royaltyRecordRepository.save(record)
                    rowCount[0]++

                    log.debug("행 $rowNumber 저장 완료: $matchedData")
                }
            )

            EasyExcel.read(tempFile, listener)
                .sheet()
                .doRead()

            // 업로드 정보 업데이트
            upload.totalRows = rowCount[0]
            upload.processedRows = rowCount[0]
            upload.status = "COMPLETED"

            log.info("엑셀 파싱 완료: ${file.originalFilename}, 타입=$type, 총 ${rowCount[0]}행")

            return excelUploadRepository.save(upload)

        } catch (e: Exception) {
            log.error("엑셀 파싱 실패", e)

            upload.status = "FAILED"
            upload.errorMessage = e.message

            return excelUploadRepository.save(upload)
        } finally {
            tempFile.delete()
        }
    }

    private fun matchHeaders(
        rowData: Map<Int, String?>,
        headers: Map<Int, String>,
        columnMappings: Map<String, String>
    ): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        rowData.forEach { (colIndex, value) ->
            val excelHeader = headers[colIndex]
            val dbColumn = columnMappings[excelHeader]

            if (dbColumn != null) {
                result[dbColumn] = value
            }
        }

        return result
    }

    private fun createTempFile(file: MultipartFile): File {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "excel-upload")
        tempDir.mkdirs()

        val tempFile = File(tempDir, "${UUID.randomUUID()}_${file.originalFilename}")
        file.inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }
}
