package com.example.redis.shared.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * Redis 공통 설정
 *
 * ws-gateway / api-server 양쪽에서 사용하는 공통 Bean 정의.
 * 각 서버 모듈은 이 설정을 ComponentScan 대상에 포함시켜야 함
 * (공통 package com.example.redis.shared.config 사용).
 */
@Configuration
class RedisConfig {

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
        val template = StringRedisTemplate(connectionFactory)
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = StringRedisSerializer()
        return template
    }

    /**
     * Redis Pub/Sub 구독 컨테이너.
     * ws-gateway는 이 컨테이너에 리스너를 등록해서 채팅방 메시지를 수신.
     * api-server는 발행만 하므로 이 컨테이너를 직접 사용하지 않음 (등록만 됨).
     */
    @Bean
    fun redisMessageListenerContainer(connectionFactory: RedisConnectionFactory): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)
        return container
    }
}
