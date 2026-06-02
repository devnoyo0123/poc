package com.example.royalty.excel

import com.alibaba.excel.annotation.ExcelProperty

/**
 * EasyExcel 테스트용 데이터 모델
 */
data class RoyaltyExcelData(
    @ExcelProperty("저작권료")
    var royaltyFee: Int? = null,

    @ExcelProperty("저작자")
    var copyrightHolder: String? = null,

    @ExcelProperty("작품명")
    var workTitle: String? = null,

    @ExcelProperty("사용일")
    var usageDate: String? = null,

    @ExcelProperty("판매량")
    var salesCount: Int? = null
)
