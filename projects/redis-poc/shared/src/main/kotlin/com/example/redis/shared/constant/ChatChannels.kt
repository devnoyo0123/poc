package com.example.redis.shared.constant

/**
 * Redis Pub/Sub 채널 네이밍 규칙 (ws-gateway / api-server 공통).
 *
 * 모든 인스턴스가 동일한 채널명으로 publish/subscribe 해야 메시지가 누락 없이
 * 전달되므로, 채널명 생성/파싱 로직을 shared 모듈에서 중앙화한다.
 */
object ChatChannels {

    fun room(room: String): String = "chat:$room"

    const val BROADCAST = "chat:__broadcast__"

    fun extractRoom(channel: String): String? =
        channel.takeIf { it.startsWith("chat:") }
            ?.removePrefix("chat:")
            ?.takeIf { it.isNotBlank() && it != "__broadcast__" }
}
