package com.example.debounce.controller

import com.example.debounce.service.DebounceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class DebounceController(
    private val debounceService: DebounceService
) {
    @PostMapping("/trigger/{key}")
    suspend fun trigger(@PathVariable key: String): ResponseEntity<Map<String, Any>> {
        val result = withContext(Dispatchers.IO) {
            debounceService.scheduleDebounced(key)
        }
        return ResponseEntity.ok(result)
    }

    @GetMapping("/health")
    fun health(): Map<String, String> = mapOf("status" to "UP")
}