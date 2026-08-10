package com.example.redis.wsgateway.handler

import com.example.redis.shared.constant.ChatChannels
import com.example.redis.shared.dto.ChatBroadcast
import com.example.redis.shared.dto.ChatInput
import com.example.redis.shared.identity.ServerIdentity
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket 커넥션 전담 핸들러 (ws-gateway 모듈).
 *
 * 역할:
 *   - 클라이언트 WS 연결을 로컬 메모리(sessions)로 유지
 *   - 클라이언트 → Redis Pub/Sub publish (stateless 발행)
 *   - Redis Pub/Sub subscribe → 로컬 세션에 브로드캐스트
 *
 * 다중 ws-gateway 인스턴스 환경에서, 각 인스턴스는 자기 세션만 관리하지만
 * Redis Pub/Sub을 통해 모든 인스턴스에 메시지가 전달되므로
 * 어느 서버에 연결된 클라이언트든 같은 방의 메시지를 받을 수 있다.
 *
 * 주의: sessions와 subscribedRooms는 인스턴스 로컬 상태.
 * 서버 장애 시 이 인스턴스에 연결된 세션은 끊어지며, 클라이언트는 재연결해야 한다
 * (nginx가 살아있는 다른 ws-gateway 인스턴스로 라우팅).
 */
@Component
class ChatWebSocketHandler(
    private val redisTemplate: RedisTemplate<String, String>,
    private val listenerContainer: RedisMessageListenerContainer,
    private val objectMapper: ObjectMapper,
    private val serverIdentity: ServerIdentity
) : TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(javaClass)
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val subscribedRooms = ConcurrentHashMap<String, MessageListener>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions[session.id] = session
        log.info("[{}] WS connected: sessionId={} (localTotal={})",
            serverIdentity.id, session.id, sessions.size)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val input = objectMapper.readValue(message.payload, ChatInput::class.java)
        subscribeRoom(input.room)

        val broadcast = ChatBroadcast(
            room = input.room,
            sender = input.sender,
            content = input.content,
            originServer = serverIdentity.id,
            originSourceType = ChatBroadcast.SourceType.WEBSOCKET,
            timestamp = System.currentTimeMillis()
        )
        val payload = objectMapper.writeValueAsString(broadcast)
        redisTemplate.convertAndSend(ChatChannels.room(input.room), payload)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions.remove(session.id)
        log.info("[{}] WS disconnected: sessionId={} (localTotal={})",
            serverIdentity.id, session.id, sessions.size)
    }

    fun getLocalSessionCount(): Int = sessions.size

    fun getSubscribedRooms(): Set<String> = subscribedRooms.keys.toSet()

    private fun subscribeRoom(room: String) {
        if (subscribedRooms.containsKey(room)) return

        val listener = MessageListener { message: Message, _ ->
            val body = String(message.body)
            sessions.values.forEach { session ->
                runCatching {
                    if (session.isOpen) session.sendMessage(TextMessage(body))
                }.onFailure { e ->
                    log.error("[{}] Failed to send to session {}: {}",
                        serverIdentity.id, session.id, e.message)
                }
            }
        }

        val channel = ChatChannels.room(room)
        listenerContainer.addMessageListener(listener, ChannelTopic(channel))
        subscribedRooms[room] = listener
        log.info("[{}] Subscribed to Redis channel: {}", serverIdentity.id, channel)
    }
}
