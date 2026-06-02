package com.example.concurrency.controller

import com.example.concurrency.entity.UserAccount
import com.example.concurrency.service.UserAccountService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserAccountController(
    private val service: UserAccountService
) {
    @PostMapping("/simple")
    fun createSimple(
        @RequestParam email: String,
        @RequestParam username: String,
        @RequestParam fullName: String
    ): ResponseEntity<Any> {
        return try {
            val user = service.createUserAccountSimple(email, username, fullName)
            ResponseEntity.ok(mapOf("success" to true, "user" to user))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("success" to false, "error" to e.message))
        }
    }

    @PostMapping("/check")
    fun createWithCheck(
        @RequestParam email: String,
        @RequestParam username: String,
        @RequestParam fullName: String
    ): ResponseEntity<Any> {
        return try {
            val user = service.createUserAccountWithCheck(email, username, fullName)
            if (user != null) {
                ResponseEntity.ok(mapOf("success" to true, "user" to user))
            } else {
                ResponseEntity.ok(mapOf("success" to false, "message" to "Already exists"))
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("success" to false, "error" to e.message))
        }
    }

    @GetMapping("/stats")
    fun getStats(@RequestParam email: String): ResponseEntity<Map<String, Int>> {
        return ResponseEntity.ok(service.getStats(email))
    }

    @GetMapping("/count")
    fun count(): ResponseEntity<Map<String, Long>> {
        return ResponseEntity.ok(mapOf("count" to service.count()))
    }

    @GetMapping("/{email}")
    fun findByEmail(@PathVariable email: String): ResponseEntity<Any> {
        val user = service.findByEmail(email)
        return if (user != null) {
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
