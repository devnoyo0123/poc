package com.example.royalty.controller

import com.example.royalty.dto.ExcelUploadDetailResponse
import com.example.royalty.dto.ExcelUploadResponse
import com.example.royalty.service.ExcelUploadService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/excel")
class ExcelUploadController(
    private val excelUploadService: ExcelUploadService
) {

    @PostMapping("/upload")
    fun uploadExcel(
        @RequestParam type: String,
        @RequestParam file: MultipartFile
    ): ResponseEntity<ExcelUploadResponse> {
        val response = excelUploadService.uploadExcel(type, file)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/uploads/{uploadId}")
    fun getUploadDetail(@PathVariable uploadId: UUID): ResponseEntity<ExcelUploadDetailResponse> {
        val response = excelUploadService.getUploadDetail(uploadId)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/types")
    fun getTypes(): ResponseEntity<Map<String, String>> {
        val types = mapOf(
            "A" to "음반협회",
            "B" to "방송협회",
            "C" to "영화협회",
            "D" to "공연협회"
        )
        return ResponseEntity.ok(types)
    }
}
