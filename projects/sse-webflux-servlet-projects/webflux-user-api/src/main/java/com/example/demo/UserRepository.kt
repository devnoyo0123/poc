package com.example.demo

import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface UserRepository : R2dbcRepository<User, Long> {

    fun findByUsername(username: String): Mono<User>
    fun findByEmail(email: String): Mono<User>
    fun findByUsernameContainingIgnoreCase(username: String): Flux<User>
}
