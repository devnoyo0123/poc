package com.example.webfluxsselab.controller

import com.example.webfluxsselab.model.User
import com.example.webfluxsselab.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {

    /**
     * 실습 4: 일반 REST API (suspend 함수)
     *
     * 테스트 방법:
     * curl http://localhost:8080/api/users/1
     */
    @GetMapping("/{id}")
    suspend fun getUser(@PathVariable id: Long): ResponseEntity<User> {
        val user = userService.findById(id)
        return if (user != null) {
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 실습 5: 목록 조회 (suspend 함수)
     *
     * 테스트 방법:
     * curl http://localhost:8080/api/users
     */
    @GetMapping
    suspend fun getUsers(): ResponseEntity<List<User>> {
        val users = userService.findAll()
        return ResponseEntity.ok(users)
    }

    /**
     * 실습 6: 사용자 생성 (suspend 함수)
     *
     * 테스트 방법:
     * curl -X POST http://localhost:8080/api/users \
     *   -H "Content-Type: application/json" \
     *   -d '{"id":1,"name":"John","email":"john@example.com"}'
     */
    @PostMapping
    suspend fun createUser(@RequestBody user: User): ResponseEntity<User> {
        val created = userService.save(user)
        return ResponseEntity.ok(created)
    }
}
