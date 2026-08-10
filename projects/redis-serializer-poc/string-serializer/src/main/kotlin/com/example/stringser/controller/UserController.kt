package com.example.stringser.controller

import com.example.stringser.domain.User
import com.example.stringser.service.UserCacheService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class UserRequest(
    val id: String,
    val name: String,
    val email: String,
    val age: Int,
)

data class RawResponse(
    val serializer: String,
    val key: String,
    val rawValue: String?,
)

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userCacheService: UserCacheService,
) {

    @PostMapping
    fun save(@RequestBody req: UserRequest): ResponseEntity<User> {
        val user = User(id = req.id, name = req.name, email = req.email, age = req.age)
        userCacheService.save(user)
        return ResponseEntity.ok(user)
    }

    @GetMapping("/{id}")
    fun find(@PathVariable id: String): ResponseEntity<User> {
        val user = userCacheService.find(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: String): ResponseEntity<Unit> {
        userCacheService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/raw")
    fun raw(@PathVariable id: String): ResponseEntity<RawResponse> {
        return ResponseEntity.ok(
            RawResponse(
                serializer = "StringRedisSerializer",
                key = "string:user:$id",
                rawValue = userCacheService.raw(id),
            )
        )
    }
}
