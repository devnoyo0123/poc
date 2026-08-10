package com.example.debounce.controller

import com.example.debounce.service.CollisionTestService
import com.example.debounce.service.DebounceService
import com.example.debounce.service.SoftDeleteDebounceService
import com.example.debounce.service.SoftDebounceExecutionTestService
import com.example.debounce.service.SucceededDeletionTestService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class DebounceController(
    private val debounceService: DebounceService,
    private val softDeleteDebounceService: SoftDeleteDebounceService,
    private val collisionTestService: CollisionTestService,
    private val succeededDeletionTestService: SucceededDeletionTestService,
    private val softDebounceExecutionTestService: SoftDebounceExecutionTestService
) {
    @PostMapping("/trigger/{key}")
    suspend fun trigger(@PathVariable key: String): ResponseEntity<Map<String, Any>> {
        val result = withContext(Dispatchers.IO) {
            debounceService.scheduleDebounced(key)
        }
        return ResponseEntity.ok(result)
    }

    @PostMapping("/trigger-soft/{key}")
    suspend fun triggerSoft(@PathVariable key: String): ResponseEntity<Map<String, Any>> {
        val result = withContext(Dispatchers.IO) {
            softDeleteDebounceService.scheduleDebounced(key)
        }
        return ResponseEntity.ok(result)
    }

    @PostMapping("/test-collision/{key}")
    suspend fun testCollision(@PathVariable key: String): ResponseEntity<Map<String, Any>> {
        val result = withContext(Dispatchers.IO) {
            collisionTestService.run(key)
        }
        return ResponseEntity.ok(result)
    }

    @PostMapping("/test-succeeded-deletion/{key}")
    suspend fun testSucceededDeletion(@PathVariable key: String): ResponseEntity<Map<String, Any>> {
        val result = withContext(Dispatchers.IO) {
            succeededDeletionTestService.run(key)
        }
        return ResponseEntity.ok(result)
    }

    @PostMapping("/test-soft-execution/{key}")
    suspend fun testSoftExecution(@PathVariable key: String): ResponseEntity<Map<String, Any>> {
        val result = withContext(Dispatchers.IO) {
            softDebounceExecutionTestService.run(key)
        }
        return ResponseEntity.ok(result)
    }

    @GetMapping("/health")
    fun health(): Map<String, String> = mapOf("status" to "UP")
}