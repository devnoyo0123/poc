package com.example.demo

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {

    @GetMapping
    fun getAllUsers(): Flux<User> =
        userService.getAllUsers()

    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): Mono<User> =
        userService.getUserById(id)

    @GetMapping("/search")
    fun searchByUsername(@RequestParam username: String): Flux<User> =
        userService.searchByUsername(username)

    @PostMapping
    fun createUser(@RequestBody request: CreateUserRequest): Mono<ResponseEntity<User>> =
        userService.createUser(request)
            .map { ResponseEntity.status(HttpStatus.CREATED).body(it) }

    @PutMapping("/{id}")
    fun updateUser(
        @PathVariable id: Long,
        @RequestBody request: UpdateUserRequest
    ): Mono<User> =
        userService.updateUser(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUser(@PathVariable id: Long): Mono<Void> =
        userService.deleteUser(id)

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAllUsers(): Mono<Void> =
        userService.deleteAllUsers()

    // Exception Handlers
    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(e: UserNotFoundException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(mapOf("error" to (e.message ?: "User not found")))

    @ExceptionHandler(DuplicateUserException::class)
    fun handleDuplicateUser(e: DuplicateUserException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(mapOf("error" to (e.message ?: "Duplicate user")))
}
