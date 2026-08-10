package com.example.stringser.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean
    @Primary
    fun userRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
        return RedisTemplate<String, String>().apply {
            setConnectionFactory(connectionFactory)
            keySerializer = StringRedisSerializer.UTF_8
            valueSerializer = StringRedisSerializer.UTF_8
            hashKeySerializer = StringRedisSerializer.UTF_8
            hashValueSerializer = StringRedisSerializer.UTF_8
        }
    }
}


