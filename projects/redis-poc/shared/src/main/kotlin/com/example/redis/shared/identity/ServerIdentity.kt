package com.example.redis.shared.identity

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 서버 인스턴스 식별자.
 *
 * 여러 ws-gateway / api-server 인스턴스가 떠 있을 때, 메시지의 originServer 필드를
 * 채우기 위해 사용. 우선순위:
 *   1. application property `app.instance-id` (Docker 등에서 hostname 주입)
 *   2. 환경변수 `INSTANCE_ID`
 *   3. 자동 생성 `${role}-${random8}`
 *
 * 운영에서는 Docker/K8s hostname이 주입되는 것이 전제.
 */
@Component
class ServerIdentity(
    @Value("\${app.instance-id:}") private val configuredId: String = "",
    @Value("\${spring.application.name:unknown}") private val applicationName: String = "unknown"
) {
    val id: String = when {
        configuredId.isNotBlank() -> configuredId
        System.getenv("INSTANCE_ID")?.isNotBlank() == true -> System.getenv("INSTANCE_ID")
        else -> "${applicationName}-${UUID.randomUUID().toString().take(8)}"
    }
}
