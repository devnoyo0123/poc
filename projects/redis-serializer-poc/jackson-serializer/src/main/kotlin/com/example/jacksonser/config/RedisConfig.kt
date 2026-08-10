package com.example.jacksonser.config

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        // 학습용 POC: 모든 타입 허용 (운영 환경에선 화이트리스트 권장 — Jackson 역직렬화 RCE 위험)
        val typeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(Any::class.java)
            .build()

        val kotlinAwareMapper = ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            activateDefaultTypingAsProperty(
                typeValidator,
                ObjectMapper.DefaultTyping.EVERYTHING,
                "@class",
            )
            setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
        }

        return RedisTemplate<String, Any>().apply {
            setConnectionFactory(connectionFactory)
            keySerializer = StringRedisSerializer.UTF_8
            valueSerializer = GenericJackson2JsonRedisSerializer(kotlinAwareMapper)
            hashKeySerializer = StringRedisSerializer.UTF_8
            hashValueSerializer = GenericJackson2JsonRedisSerializer(kotlinAwareMapper)
        }
    }
}
