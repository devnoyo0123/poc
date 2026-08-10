package com.example.stringser.service

import com.example.stringser.domain.User
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class UserCacheService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {

    private val keyPrefix = "string:user:"
    private val ttl: Duration = Duration.ofMinutes(60)

    fun save(user: User) {
        val key = "$keyPrefix${user.id}"
        val json = objectMapper.writeValueAsString(user)
        redisTemplate.opsForValue().set(key, json, ttl)
    }

    fun find(id: String): User? {
        val json = redisTemplate.opsForValue().get("$keyPrefix$id") ?: return null
        return objectMapper.readValue(json, User::class.java)
    }

    fun delete(id: String): Boolean {
        return redisTemplate.delete("$keyPrefix$id")
    }

    fun raw(id: String): String? {
        return redisTemplate.opsForValue().get("$keyPrefix$id")
    }
}
