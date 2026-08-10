package com.example.redis.apiserver.controller

import com.example.redis.shared.constant.ChatChannels
import com.example.redis.shared.dto.ChatBroadcast
import com.example.redis.shared.dto.ChatInput
import com.example.redis.shared.identity.ServerIdentity
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.*

/**
 * 채팅 메시지 발행 전용 REST 컨트롤러.
 *
 * Stateless: 이 서버는 어떤 WS 세션도 유지하지 않는다. 단지 클라이언트의
 * HTTP 요청을 받아 Redis Pub/Sub으로 publish만 수행. 실제 클라이언트 전달은
 * ws-gateway 인스턴스들이 담당한다.
 *
 * 사용 예: 배치 시스템이나 CI/CD에서 프로그래밍 방식으로 채팅방에 메시지를
 * 보내야 할 때 유용 (WS 클라이언트를 띄우지 않고도).
 */
@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val serverIdentity: ServerIdentity
) {

    @PostMapping("/send")
    fun send(@RequestBody input: ChatInput): Map<String, Any> {
        val broadcast = ChatBroadcast(
            room = input.room,
            sender = input.sender,
            content = input.content,
            originServer = serverIdentity.id,
            originSourceType = ChatBroadcast.SourceType.REST_API,
            timestamp = System.currentTimeMillis()
        )
        val payload = objectMapper.writeValueAsString(broadcast)
        val channel = ChatChannels.room(input.room)
        val receivers = redisTemplate.convertAndSend(channel, payload) ?: 0L

        return mapOf(
            "channel" to channel,
            "receivers" to receivers,
            "originServer" to serverIdentity.id,
            "originSourceType" to "REST_API"
        )
    }

    @GetMapping("/channels/{room}")
    fun channelInfo(@PathVariable room: String): Map<String, Any> = mapOf(
        "room" to room,
        "redisChannel" to ChatChannels.room(room)
    )
}
