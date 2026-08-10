package com.example.redis.shared.dto

/**
 * 채팅 메시지 DTO 모음.
 *
 * 메시지 흐름:
 *   Client → [ChatInput] → ws-gateway 또는 api-server
 *         → [ChatBroadcast] via Redis Pub/Sub
 *         → 모든 ws-gateway 인스턴스 → 각자 로컬 세션에 전달
 *
 * 입력값(클라이언트)과 브로드캐스트값(서버 풍부화)을 분리한 이유:
 * originServer / originSourceType / timestamp는 서버에서만 채워지는 추적 메타데이터.
 */
data class ChatInput(
    val room: String = "",
    val sender: String = "",
    val content: String = ""
)

data class ChatBroadcast(
    val room: String,
    val sender: String,
    val content: String,
    val originServer: String,
    val originSourceType: SourceType,
    val timestamp: Long
) {
    enum class SourceType { WEBSOCKET, REST_API }
}
