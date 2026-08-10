package com.career.pathenum.model.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateCommentRequest(
    @field:NotNull val postId: Long,
    val parentId: Long?,  // null 이면 루트 댓글
    @field:NotBlank val content: String
)

data class CommentResponse(
    val id: Long,
    val postId: Long,
    val parentId: Long?,
    val path: String,
    val depth: Int,
    val content: String,
    val createdAt: String,
    val updatedAt: String?
)

// 트리 형태 응답 (옵션)
data class CommentTreeNode(
    val comment: CommentResponse,
    val children: List<CommentTreeNode> = emptyList()
)
