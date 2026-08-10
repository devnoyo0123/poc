package com.example.jacksonser.service

import com.example.jacksonser.domain.User
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class UserCacheService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
) {

    private val keyPrefix = "jackson:user:"
    private val ttl: Duration = Duration.ofMinutes(60)

    fun save(user: User) {
        redisTemplate.opsForValue().set("$keyPrefix${user.id}", user, ttl)
    }

    fun find(id: String): User? {
        val value = redisTemplate.opsForValue().get("$keyPrefix$id") ?: return null
        return value as? User
    }

    fun delete(id: String): Boolean {
        return redisTemplate.delete("$keyPrefix$id")
    }

    fun raw(id: String): String? {
        val value = redisTemplate.opsForValue().get("$keyPrefix$id") ?: return null
        return objectMapper.writeValueAsString(value)
    }
}
