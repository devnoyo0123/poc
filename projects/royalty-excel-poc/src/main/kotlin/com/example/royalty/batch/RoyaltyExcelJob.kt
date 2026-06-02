package com.example.royalty.batch

import com.alibaba.excel.EasyExcel
import com.example.royalty.entity.ExcelTypeMapping
import com.example.royalty.entity.RoyaltyRecord
import com.example.royalty.excel.listener.RoyaltyExcelListener
import com.example.royalty.repository.ExcelTypeMappingRepository
import com.example.royalty.repository.RoyaltyRecordRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import software.amazon.awssdk.services.s3.S3Client
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.Sheet
import java.io.File
import java.util.UUID

@Configuration
class RoyaltyExcelJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val s3Client: S3Client,
    private val royaltyRecordRepository: RoyaltyRecordRepository,
    private val excelTypeMappingRepository: ExcelTypeMappingRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun processExcelFileJob(): Job {
        return JobBuilder("processExcelFileJob", jobRepository)
            .start(processExcelStep())
            .build()
    }

    @Bean
    fun processExcelStep(): Step {
        return StepBuilder("processExcelStep", jobRepository)
            .tasklet(excelProcessingTasklet(), transactionManager)
            .build()
    }

    @StepScope
    @Bean
    fun excelProcessingTasklet(): Tasklet {
        return Tasklet { contribution, chunkContext ->
            // Spring Batch 5.x: ExecutionContext에서 직접 접근
            val jobParameters = chunkContext.stepContext.jobParameters
            val s3Key = jobParameters["s3Key"]?.toString()
                ?: throw IllegalArgumentException("s3Key is required")

            val type = jobParameters["type"]?.toString()
                ?: throw IllegalArgumentException("type is required")

            val uploadId = UUID.fromString(
                jobParameters["uploadId"]?.toString()
                    ?: throw IllegalArgumentException("uploadId is required")
            )

            log.info("배치 처리 시작: s3Key=$s3Key, type=$type, uploadId=$uploadId")

            // 타입 매핑 조회
            val typeMapping = excelTypeMappingRepository.findByType(type)
                ?: throw IllegalArgumentException("알 수 없는 타입: $type")

            // S3에서 파일 다운로드
            val tempFile = downloadFromS3(s3Key)

            try {
                val totalRowCount = intArrayOf(0)
                val batchSize = 2000
                val buffer = mutableListOf<RoyaltyRecord>()

                // ✅ Apache POI로 시트 목록 가져오기
                val workbook = WorkbookFactory.create(tempFile)
                val sheets = mutableListOf<String>()

                workbook.use { wb ->
                    log.info("엑셀 파일 분석 시작...")

                    for (i in 0 until wb.numberOfSheets) {
                        val sheet = wb.getSheetAt(i)
                        val sheetName = sheet.sheetName
                        sheets.add(sheetName)
                        log.info("시트 ${i + 1}: $sheetName (${sheet.lastRowNum}행)")
                    }
                }

                log.info("총 ${sheets.size}개 시트 발견, 처리 시작")

                // ✅ 각 시트 순회하며 처리
                sheets.forEach { sheetName ->
                    log.info("시트 처리 시작: $sheetName")

                    val sheetRowCount = intArrayOf(0)

                    // EasyExcel로 파싱
                    val listener = RoyaltyExcelListener(
                        typeMapping = typeMapping,
                        headerRowStart = typeMapping.headerRowStart,
                        sheetName = sheetName,  // ✅ 시트 이름 전달
                        onDataRead = { rowNumber, rowData, headers ->
                            // 헤더 매칭 및 데이터 변환
                            val matchedData = matchHeaders(rowData, headers, typeMapping.columnMappings)

                            // 원본 데이터
                            val originalData = rowData.mapKeys { (colIndex, _) ->
                                headers[colIndex] ?: "col_$colIndex"
                            }

                            // DB 저장 (시트 이름 포함)
                            val record = RoyaltyRecord(
                                uploadId = uploadId,
                                sheetName = sheetName,  // ✅ 시트 이름 저장
                                rowNumber = rowNumber,
                                originalData = originalData,
                                matchedData = matchedData,
                                headerMapping = typeMapping.columnMappings
                            )

                            buffer.add(record)
                            sheetRowCount[0]++
                            totalRowCount[0]++

                            log.debug("[$sheetName] 행 $rowNumber 저장 완료")

                            // ✅ 배치 크기만큼 모으면 한 번에 저장
                            if (buffer.size >= batchSize) {
                                royaltyRecordRepository.saveAll(buffer)
                                log.debug("배치 저장: ${buffer.size}행 완료 (누적: ${totalRowCount[0]}행)")
                                buffer.clear()
                            }
                        }
                    )

                    EasyExcel.read(tempFile, listener)
                        .sheet(sheetName)  // ✅ 특정 시트 읽기 (String)
                        .doRead()

                    log.info("[$sheetName] 처리 완료: ${sheetRowCount[0]}행 저장됨")
                }

                // ✅ 남은 데이터 저장
                if (buffer.isNotEmpty()) {
                    royaltyRecordRepository.saveAll(buffer)
                    log.debug("마지막 배치 저장: ${buffer.size}행 완료")
                }

                log.info("배치 처리 완료: 총 ${totalRowCount[0]}행 저장됨 (${sheets.size}개 시트)")

            } finally {
                tempFile.delete()
            }

            RepeatStatus.FINISHED
        }
    }

    private fun downloadFromS3(s3Key: String): File {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "batch-excel")
        tempDir.mkdirs()

        val tempFile = File(tempDir, "${UUID.randomUUID()}.xlsx")

        val s3Response = s3Client.getObject(
            software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                .bucket("royalty-excel-uploads")
                .key(s3Key)
                .build()
        )

        s3Response.use { s3Stream ->
            tempFile.outputStream().use { output ->
                s3Stream.transferTo(output)
            }
        }

        log.info("S3 다운로드 완료: $s3Key → ${tempFile.absolutePath}")

        return tempFile
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
}
