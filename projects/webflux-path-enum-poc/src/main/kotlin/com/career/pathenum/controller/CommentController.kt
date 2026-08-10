package com.career.pathenum.controller

import com.career.pathenum.model.dto.DeleteResponse
import com.career.pathenum.model.dto.MoveRequest
import com.career.pathenum.model.dto.MoveResponse
import com.career.pathenum.model.dto.toResponse
import com.career.pathenum.service.CommentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 댓글 REST API.
 *
 * 엔드포인트 매트릭스:
 *   POST   /api/comments                 - 생성
 *   GET    /api/comments/{id}            - 단건 조회
 *   GET    /api/comments/posts/{postId}  - 게시글 트리 조회
 *   GET    /api/comments/{id}/descendants - 자손 전체
 *   GET    /api/comments/{id}/ancestors    - 조상 전체
 *   DELETE /api/comments/{id}            - 자손 포함 일괄 삭제
 *   PATCH  /api/comments/{id}/move       - 부모 이동
 */
@RestController
@RequestMapping("/api/comments")
class CommentController(private val service: CommentService) {

    @PostMapping
    suspend fun create(
        @RequestBody @Valid req: com.career.pathenum.model.dto.CreateCommentRequest
    ): ResponseEntity<com.career.pathenum.model.dto.CommentResponse> {
        val saved = service.createComment(req)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: Long): com.career.pathenum.model.dto.CommentResponse =
        service.getComment(id).toResponse()

    @GetMapping("/posts/{postId}")
    suspend fun getTree(
        @PathVariable postId: Long
    ): List<com.career.pathenum.model.dto.CommentTreeNode> = service.getCommentTree(postId)

    @GetMapping("/{id}/descendants")
    suspend fun getDescendants(
        @PathVariable id: Long
    ): List<com.career.pathenum.model.dto.CommentResponse> =
        service.getDescendants(id).map { it.toResponse() }

    @GetMapping("/{id}/ancestors")
    suspend fun getAncestors(
        @PathVariable id: Long
    ): List<com.career.pathenum.model.dto.CommentResponse> =
        service.getAncestors(id).map { it.toResponse() }

    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable id: Long): DeleteResponse =
        DeleteResponse(service.deleteComment(id))

    @PatchMapping("/{id}/move")
    suspend fun move(
        @PathVariable id: Long,
        @RequestBody req: MoveRequest
    ): MoveResponse = MoveResponse(service.moveComment(id, req.newParentId))
}
