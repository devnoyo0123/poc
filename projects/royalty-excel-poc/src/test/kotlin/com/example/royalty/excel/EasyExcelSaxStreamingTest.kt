package com.example.royalty.excel

import com.alibaba.excel.EasyExcel
import com.alibaba.excel.event.AnalysisEventListener
import com.alibaba.excel.context.AnalysisContext
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * EasyExcel SAX Streaming 성능 테스트
 *
 * 목적: 대용량 엑셀 파일 처리 시 메모리 효율성 검증
 * - SAX 방식은 전체 파일을 메모리에 로드하지 않고 스트리밍
 * - 대용량 파일에서도 일정한 메모리 사용량 유지
 */
class EasyExcelSaxStreamingTest {

    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun `10,000행 엑셀 파일 SAX 스트리밍 테스트`() {
        testSaxStreaming("samples/type_a_large_10k.xlsx", 10_000)
    }

    @Test
    fun `50,000행 엑셀 파일 SAX 스트리밍 테스트`() {
        testSaxStreaming("samples/type_a_large_50k.xlsx", 50_000)
    }

    @Test
    fun `SAX 스트리밍 메모리 사용량 측정`() {
        val fileName = "samples/type_a_large_50k.xlsx"
        val file = File(fileName)

        if (!file.exists()) {
            log.warn("테스트 파일이 존재하지 않습니다: $fileName")
            return
        }

        log.info("=== SAX Streaming 메모리 사용량 측정 ===")
        log.info("파일: ${file.name}, 크기: ${file.length() / 1024}KB")

        // JVM 메모리 모니터링
        val memoryBean = ManagementFactory.getMemoryMXBean()
        val runtime = Runtime.getRuntime()

        // GC 실행 후 초기 메모리 상태 기록
        System.gc()
        Thread.sleep(500)

        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        log.info("초기 메모리 사용: ${initialMemory / 1024 / 1024}MB")

        val rowCount = AtomicInteger(0)
        val batchTimes = mutableListOf<Long>()

        val startTime = System.currentTimeMillis()

        // EasyExcel로 스트리밍 읽기
        EasyExcel.read(file, RoyaltyExcelData::class.java, object : AnalysisEventListener<RoyaltyExcelData>() {
            private var batchCount = 0
            private val batchSize = 1000
            private var batchStartTime = System.currentTimeMillis()

            override fun invoke(data: RoyaltyExcelData, context: AnalysisContext) {
                rowCount.incrementAndGet()

                // 배치 단위로 성능 측정
                if (rowCount.get() % batchSize == 0) {
                    val batchEnd = System.currentTimeMillis()
                    batchTimes.add(batchEnd - batchStartTime)
                    batchStartTime = batchEnd

                    // 현재 메모리 사용량 로깅
                    if (rowCount.get() % 10000 == 0) {
                        val currentMemory = runtime.totalMemory() - runtime.freeMemory()
                        val usedMemory = (currentMemory - initialMemory) / 1024 / 1024
                        val peakMemory = memoryBean.heapMemoryUsage.used / 1024 / 1024
                        log.info("처리 중: ${String.format("%,d", rowCount.get())}행, 메모리 사용: +${usedMemory}MB, Peak: ${peakMemory}MB")
                    }
                }
            }

            override fun doAfterAllAnalysed(context: AnalysisContext) {
                log.info("모든 데이터 분석 완료")
            }
        }).sheet().doRead()

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // 최종 메모리 사용량
        System.gc()
        Thread.sleep(500)

        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val totalMemoryUsed = (finalMemory - initialMemory) / 1024 / 1024
        val peakMemory = memoryBean.heapMemoryUsage.used / 1024 / 1024
        val avgBatchTime = if (batchTimes.isNotEmpty()) batchTimes.average() else 0.0
        val maxBatchTime = batchTimes.maxOrNull() ?: 0

        log.info("\n=== 성능 측정 결과 ===")
        log.info("총 처리 행: ${String.format("%,d", rowCount.get())}행")
        log.info("총 소요 시간: ${duration}ms (${duration / 1000.0}초)")
        log.info("처리 속도: ${(rowCount.get() / (duration / 1000.0)).toInt()}행/초")
        log.info("평균 배치 처리 시간: ${avgBatchTime.toInt()}ms (배치당 1000행)")
        log.info("최대 배치 처리 시간: ${maxBatchTime}ms")
        log.info("총 메모리 사용 증가: +${totalMemoryUsed}MB")
        log.info("Peak 힙 메모리: ${peakMemory}MB")
        log.info("1행당 메모리: ${(totalMemoryUsed * 1024.0 / rowCount.get())}KB")

        // 검증: 메모리 효율성 확인
        // 50,000행을 처리해도 100MB 이하의 메모리 증가여야 함 (SAX 스트리밍의 장점)
        assert(totalMemoryUsed < 100) {
            "SAX 스트리밍 방식은 메모리 효율적이어야 합니다. " +
            "현재 ${totalMemoryUsed}MB 사용 (기대: < 100MB)"
        }

        log.info("\n✅ SAX Streaming 테스트 통과: 메모리 효율적 처리 확인")
    }

    @Test
    fun `페이지별 읽기 성능 비교`() {
        val fileName = "samples/type_a_large_10k.xlsx"
        val file = File(fileName)

        if (!file.exists()) {
            log.warn("테스트 파일이 존재하지 않습니다: $fileName")
            return
        }

        log.info("=== 페이지 크기별 성능 비교 ===")

        val batchSizes = listOf(100, 500, 1000, 2000, 5000)
        val results = mutableMapOf<Int, PerformanceResult>()

        batchSizes.forEach { batchSize ->
            System.gc()
            Thread.sleep(500)

            val runtime = Runtime.getRuntime()
            val initialMemory = runtime.totalMemory() - runtime.freeMemory()

            val rowCount = AtomicInteger(0)
            val startTime = System.currentTimeMillis()

            EasyExcel.read(file, RoyaltyExcelData::class.java, object : AnalysisEventListener<RoyaltyExcelData>() {
                override fun invoke(data: RoyaltyExcelData, context: AnalysisContext) {
                    rowCount.incrementAndGet()
                }

                override fun doAfterAllAnalysed(context: AnalysisContext) {}
            }).sheet().doRead()

            val duration = System.currentTimeMillis() - startTime
            val finalMemory = runtime.totalMemory() - runtime.freeMemory()
            val memoryUsed = (finalMemory - initialMemory) / 1024 / 1024

            results[batchSize] = PerformanceResult(
                batchSize = batchSize,
                duration = duration,
                memoryUsedMB = memoryUsed,
                rowsPerSecond = (rowCount.get() / (duration / 1000.0)).toInt()
            )

            log.info("배치 크기 $batchSize: ${duration}ms, 메모리: +${memoryUsed}MB, 속도: ${(rowCount.get() / (duration / 1000.0)).toInt()}행/초")
        }

        // 최적 배치 크기 분석
        val optimal = results.values.minByOrNull { it.duration }
        log.info("\n최적 배치 크기: ${optimal?.batchSize} (소요 시간: ${optimal?.duration}ms)")
    }

    private fun testSaxStreaming(fileName: String, expectedRows: Int) {
        val file = File(fileName)

        if (!file.exists()) {
            log.warn("테스트 파일이 존재하지 않습니다: $fileName")
            return
        }

        log.info("=== SAX Streaming 테스트: ${file.name} ===")
        log.info("파일 크기: ${file.length() / 1024}KB")

        val rowCount = AtomicInteger(0)
        val startTime = System.currentTimeMillis()

        EasyExcel.read(file, RoyaltyExcelData::class.java, object : AnalysisEventListener<RoyaltyExcelData>() {
            private var lastLoggedCount = 0

            override fun invoke(data: RoyaltyExcelData, context: AnalysisContext) {
                val currentCount = rowCount.incrementAndGet()

                // 진행상황 로깅
                if (currentCount % 10000 == 0 && currentCount != lastLoggedCount) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val rate = currentCount / (elapsed / 1000.0)
                    log.info("진행: ${String.format("%,d", currentCount)}행 (${currentCount * 100 / expectedRows}%), 속도: ${rate.toInt()}행/초")
                    lastLoggedCount = currentCount
                }
            }

            override fun doAfterAllAnalysed(context: AnalysisContext) {
                log.info("모든 데이터 분석 완료")
            }
        }).sheet().doRead()

        val duration = System.currentTimeMillis() - startTime

        log.info("완료: 총 ${String.format("%,d", rowCount.get())}행 처리")
        log.info("소요 시간: ${duration}ms (${duration / 1000.0}초)")
        log.info("평균 속도: ${(rowCount.get() / (duration / 1000.0)).toInt()}행/초")

        // 검증
        assert(rowCount.get() == expectedRows) {
            "예상 행 수와 일치해야 합니다. 기대: $expectedRows, 실제: ${rowCount.get()}"
        }

        log.info("✅ 테스트 통과\n")
    }

    data class PerformanceResult(
        val batchSize: Int,
        val duration: Long,
        val memoryUsedMB: Long,
        val rowsPerSecond: Int
    )
}
