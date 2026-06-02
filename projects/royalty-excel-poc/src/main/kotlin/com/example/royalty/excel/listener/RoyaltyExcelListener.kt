package com.example.royalty.excel.listener

import com.alibaba.excel.context.AnalysisContext
import com.alibaba.excel.event.AnalysisEventListener
import com.example.royalty.entity.ExcelTypeMapping
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * EasyExcel SAX 방식 리스너
 * 대용량 엑셀을 메모리에 한 번에 올리지 않고 스트리밍으로 처리
 */
class RoyaltyExcelListener(
    private val typeMapping: ExcelTypeMapping,
    private val headerRowStart: Int,
    private val sheetName: String,  // ✅ 시트 이름 추가
    private val onDataRead: (rowNumber: Int, rowData: Map<Int, String?>, headers: Map<Int, String>) -> Unit
) : AnalysisEventListener<Map<Int, String>>() {

    private val log = LoggerFactory.getLogger(javaClass)
    private var headers: Map<Int, String> = emptyMap()
    private var currentRow = 0
    private var dataStartRow = 0

    override fun invokeHeadMap(headMap: Map<Int, String>, context: AnalysisContext) {
        val rowIndex = context.readRowHolder().rowIndex + 1 // 1-based

        // 지정된 헤더 행에 도달하면 헤더 저장
        if (rowIndex == headerRowStart) {
            headers = headMap.mapValues { it.value ?: "" }
            dataStartRow = headerRowStart + 1
            log.info("[$sheetName] 헤더 감지 (행 $rowIndex): $headers")  // ✅ 시트명 로깅
        }
    }

    override fun invoke(data: Map<Int, String>, context: AnalysisContext) {
        currentRow = context.readRowHolder().rowIndex + 1 // 1-based

        // 헤더 행 이전의 데이터는 무시
        if (currentRow < dataStartRow) {
            log.debug("[$sheetName] 헤더 행 이전 데이터 무시 (행 $currentRow)")  // ✅ 시트명 로깅
            return
        }

        // 빈 행 무시
        if (data.values.all { it.isNullOrBlank() }) {
            log.debug("[$sheetName] 빈 행 무시 (행 $currentRow)")  // ✅ 시트명 로깅
            return
        }

        log.debug("[$sheetName] 데이터 행 읽음 (행 $currentRow): $data")  // ✅ 시트명 로깅

        // 데이터 처리 콜백 호출
        onDataRead(currentRow, data, headers)
    }

    override fun doAfterAllAnalysed(context: AnalysisContext) {
        log.info("[$sheetName] 엑셀 파싱 완료. 총 $currentRow 행 처리됨")  // ✅ 시트명 로깅
    }
}
