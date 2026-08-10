package com.career.pathenum.model

import java.time.LocalDateTime

/**
 * 댓글 도메인 모델.
 * jOOQ Record 와 변환됨 (Repository 레이어에서 매핑 — Phase 2 범위).
 */
data class Comment(
    val id: Long,
    val postId: Long,
    val parentId: Long?,
    val path: String,
    val depth: Int,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?
)
