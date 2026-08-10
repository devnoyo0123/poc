package com.example.redis.apiserver.service

import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.stereotype.Service
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 일반 목적의 Redis Pub/Sub 서비스 (채팅 외 채널).
 *
 * 채팅은 ChatController에서 전용 처리(추적 메타데이터 포함)하므로
 * 이 서비스는 범용 pub/sub 데모/실시간 알림 용도로만 사용.
 */
@Service
class PubSubService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val listenerContainer: RedisMessageListenerContainer
) {

    private val messageHistory = CopyOnWriteArrayList<String>()

    fun publish(channel: String, message: String): Map<String, Any> {
        val receiverCount = redisTemplate.convertAndSend(channel, message) ?: 0L
        return mapOf(
            "channel" to channel,
            "message" to message,
            "receivers" to receiverCount
        )
    }

    fun subscribe(channel: String): Map<String, Any> {
        val listener = object : MessageListener {
            override fun onMessage(message: Message, pattern: ByteArray?) {
                val body = String(message.body)
                val channelName = String(message.channel)
                messageHistory.add("[$channelName] $body")
            }
        }

        listenerContainer.addMessageListener(listener, ChannelTopic(channel))

        return mapOf(
            "status" to "subscribed",
            "channel" to channel,
            "note" to "Messages will be stored in history. Check /api/pubsub/history"
        )
    }

    fun getMessageHistory(): List<String> = messageHistory.toList()

    fun clearHistory(): String {
        messageHistory.clear()
        return "History cleared"
    }
}
