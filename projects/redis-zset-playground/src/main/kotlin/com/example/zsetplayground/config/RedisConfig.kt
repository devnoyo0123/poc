package com.example.zsetplayground.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * Redis 설정
 * - Key/Value 모두 StringRedisSerializer 사용 (redis-cli에서 깨지지 않게)
 * - Hash도 동일하게 String 직렬화
 */
@Configuration
class RedisConfig {

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
        return RedisTemplate<String, String>().apply {
            setConnectionFactory(connectionFactory)
            keySerializer = StringRedisSerializer.UTF_8
            valueSerializer = StringRedisSerializer.UTF_8
            hashKeySerializer = StringRedisSerializer.UTF_8
            hashValueSerializer = StringRedisSerializer.UTF_8
        }
    }
}
