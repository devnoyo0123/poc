package com.example.demo

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class UserService(
    private val userRepository: UserRepository
) {

    fun getAllUsers(): Flux<User> =
        userRepository.findAll()

    fun getUserById(id: Long): Mono<User> =
        userRepository.findById(id)
            .switchIfEmpty(Mono.error(UserNotFoundException("User not found with id: $id")))

    fun searchByUsername(username: String): Flux<User> =
        userRepository.findByUsernameContainingIgnoreCase(username)

    fun createUser(request: CreateUserRequest): Mono<User> {
        return userRepository
            .findByUsername(request.username)
            .flatMap { Mono.error<User>(DuplicateUserException("Username already exists: ${request.username}")) }
            .switchIfEmpty(
                userRepository.findByEmail(request.email)
                    .flatMap { Mono.error<User>(DuplicateUserException("Email already exists: ${request.email}")) }
            )
            .switchIfEmpty(
                userRepository.save(
                    User(
                        username = request.username,
                        email = request.email
                    )
                )
            )
    }

    fun updateUser(id: Long, request: UpdateUserRequest): Mono<User> {
        return userRepository
            .findById(id)
            .switchIfEmpty(Mono.error(UserNotFoundException("User not found with id: $id")))
            .flatMap { existingUser ->
                val updatedUser = existingUser.copy(
                    username = request.username ?: existingUser.username,
                    email = request.email ?: existingUser.email
                )
                userRepository.save(updatedUser)
            }
    }

    fun deleteUser(id: Long): Mono<Void> =
        userRepository.deleteById(id)

    fun deleteAllUsers(): Mono<Void> =
        userRepository.deleteAll()
}

// Exceptions
class UserNotFoundException(message: String) : RuntimeException(message)
class DuplicateUserException(message: String) : RuntimeException(message)
