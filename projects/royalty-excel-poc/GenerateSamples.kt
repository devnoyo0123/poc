import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileOutputStream

// 타입 A: 음반협회 (헤더 1행부터)
fun createTypeA() {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("음반협회 저작권료")

    // 헤더 (1행)
    val headerRow = sheet.createRow(0)
    val headers = listOf("저작권료", "저작자", "작품명", "사용일", "판매량")
    headers.forEachIndexed { idx, title ->
        headerRow.createCell(idx).setCellValue(title)
    }

    // 데이터
    val data = listOf(
        listOf("100000", "홍길동", "Song1", "2024-01-01", "1000"),
        listOf("150000", "김철수", "Song2", "2024-01-02", "1500"),
        listOf("200000", "이영희", "Song3", "2024-01-03", "2000"),
        listOf("120000", "박민수", "Song4", "2024-01-04", "1200"),
        listOf("180000", "최수진", "Song5", "2024-01-05", "1800")
    )

    data.forEachIndexed { rowIdx, rowData ->
        val row = sheet.createRow(rowIdx + 1)
        rowData.forEachIndexed { colIdx, value ->
            row.createCell(colIdx).setCellValue(value)
        }
    }

    FileOutputStream("samples/type_a_music.xlsx").use { workbook.write(it) }
    workbook.close()
    println("타입 A 생성 완료: samples/type_a_music.xlsx")
}

// 타입 B: 방송협회 (헤더 3행부터 - 타이틀, 기간 행 있음)
fun createTypeB() {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("방송협회 정산")

    // 1행: 타이틀
    val titleRow = sheet.createRow(0)
    titleRow.createCell(0).setCellValue("방송협회 저작권료 정산 보고서")

    // 2행: 기간
    val periodRow = sheet.createRow(1)
    periodRow.createCell(0).setCellValue("정산 기간: 2024년 1월 ~ 3월")

    // 3행: 헤더
    val headerRow = sheet.createRow(2)
    val headers = listOf("지급액", "권리자", "프로그램명", "방송일", "방송시간")
    headers.forEachIndexed { idx, title ->
        headerRow.createCell(idx).setCellValue(title)
    }

    // 4행부터: 데이터
    val data = listOf(
        listOf("50000", "KBS", "뉴스9", "2024-01-01", "60"),
        listOf("75000", "MBC", "무한도전", "2024-01-05", "90"),
        listOf("100000", "SBS", "런닝맨", "2024-01-10", "85"),
        listOf("60000", "tvN", "유 퀴즈 온 더 블럭", "2024-01-15", "75"),
        listOf("80000", "JTBC", "아는 형님", "2024-01-20", "80")
    )

    data.forEachIndexed { rowIdx, rowData ->
        val row = sheet.createRow(rowIdx + 3)
        rowData.forEachIndexed { colIdx, value ->
            row.createCell(colIdx).setCellValue(value)
        }
    }

    FileOutputStream("samples/type_b_broadcast.xlsx").use { workbook.write(it) }
    workbook.close()
    println("타입 B 생성 완료: samples/type_b_broadcast.xlsx")
}

// 타입 C: 영화협회 (헤더 2행부터)
fun createTypeC() {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("영화협회 상영료")

    // 1행: 빈 행
    sheet.createRow(0)

    // 2행: 헤더
    val headerRow = sheet.createRow(1)
    val headers = listOf("상영료", "제작사", "영화제목", "개봉일", "관객수")
    headers.forEachIndexed { idx, title ->
        headerRow.createCell(idx).setCellValue(title)
    }

    // 3행부터: 데이터
    val data = listOf(
        listOf("5000000", "CJ엔터테인먼트", "범죄도시4", "2024-01-01", "1000000"),
        listOf("3000000", "NEW", "12월의 warmth", "2024-01-10", "600000"),
        listOf("4000000", "롯데엔터테인먼트", "노량: 죽음의 바다", "2024-01-15", "800000"),
        listOf("3500000", "이안스토리", "서울의 봄", "2024-01-20", "700000"),
        listOf("4500000", "필름머신", "3일의 휴일", "2024-01-25", "900000")
    )

    data.forEachIndexed { rowIdx, rowData ->
        val row = sheet.createRow(rowIdx + 2)
        rowData.forEachIndexed { colIdx, value ->
            row.createCell(colIdx).setCellValue(value)
        }
    }

    FileOutputStream("samples/type_c_movie.xlsx").use { workbook.write(it) }
    workbook.close()
    println("타입 C 생성 완료: samples/type_c_movie.xlsx")
}

// 타입 D: 공연협회 (헤더 1행부터, 컬럼 순서 다름)
fun createTypeD() {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("공연협회 공연료")

    // 헤더 (컬럼 순서가 다름)
    val headerRow = sheet.createRow(0)
    val headers = listOf("공연자", "티켓판매", "공연일", "공연료", "공연명")
    headers.forEachIndexed { idx, title ->
        headerRow.createCell(idx).setCellValue(title)
    }

    // 데이터 (컬럼 순서에 맞춰서)
    val data = listOf(
        listOf("BTS", "5000000", "2024-01-01", "100000000", "BTS WORLD TOUR"),
        listOf("BLACKPINK", "4000000", "2024-01-10", "80000000", "BLACKPINK CONCERT"),
        listOf("아이브", "3000000", "2024-01-15", "60000000", "IVE SHOW"),
        listOf("세븐틴", "4500000", "2024-01-20", "90000000", "SEVENTEEN TOUR"),
        listOf("뉴진스", "3500000", "2024-01-25", "70000000", "NEWJEANS CONCERT")
    )

    data.forEachIndexed { rowIdx, rowData ->
        val row = sheet.createRow(rowIdx + 1)
        rowData.forEachIndexed { colIdx, value ->
            row.createCell(colIdx).setCellValue(value)
        }
    }

    FileOutputStream("samples/type_d_performance.xlsx").use { workbook.write(it) }
    workbook.close()
    println("타입 D 생성 완료: samples/type_d_performance.xlsx")
}

fun main() {
    createTypeA()
    createTypeB()
    createTypeC()
    createTypeD()
    println("모든 샘플 엑셀 파일 생성 완료!")
}
