package com.career.pathenum.exception

/**
 * 댓글 시스템 도메인 예외.
 * 왜 실패했는지(NotFound vs 규칙 위반 vs 시스템)를 타입으로 표현 → GlobalExceptionHandler 가 HTTP 코드로 매핑.
 */
class CommentNotFoundException(id: Long) : RuntimeException("Comment not found: $id")

class ParentNotFoundException(parentId: Long) : RuntimeException("Parent comment not found: $parentId")

class InvalidDepthException(message: String) : RuntimeException(message)

class PostMismatchException(message: String) : RuntimeException(message)
