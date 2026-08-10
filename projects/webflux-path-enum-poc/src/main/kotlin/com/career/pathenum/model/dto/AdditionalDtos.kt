package com.career.pathenum.model.dto

import com.career.pathenum.model.Comment
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class DeleteResponse(val deletedCount: Long)

data class MoveRequest(val newParentId: Long?)

data class MoveResponse(val affected: Long)

/**
 * Comment ↔ CommentResponse 변환 확장 함수.
 * LocalDateTime → ISO-8601 문자열 직렬화는 JSON 응답 일관성을 위해 컨트롤러 경계에서만 수행한다.
 */
fun Comment.toResponse(): CommentResponse = CommentResponse(
    id = id,
    postId = postId,
    parentId = parentId,
    path = path,
    depth = depth,
    content = content,
    createdAt = formatIso(createdAt),
    updatedAt = updatedAt?.let { formatIso(it) }
)

private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

private fun formatIso(t: LocalDateTime): String = ISO.format(t)
