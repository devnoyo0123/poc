package com.career.pathenum.repository

import com.career.pathenum.model.Comment

interface CommentRepository {

    suspend fun save(comment: Comment): Comment

    suspend fun findById(id: Long): Comment?

    suspend fun findByPostIdOrderByPath(postId: Long): List<Comment>

    /** 지정한 path 로 시작하는 모든 자손(자신 포함)을 path 오름차순으로 반환. */
    suspend fun findSubtree(postId: Long, rootPath: String): List<Comment>

    /** 직속 자식만(depth = parent.depth + 1). */
    suspend fun findDirectChildren(postId: Long, parentPath: String, parentDepth: Int): List<Comment>

    /** path 세그먼트들을 모두 잘라내어 IN 조회한 조상 댓글들(루트 방향, 자신 제외). */
    suspend fun findAncestors(postId: Long, path: String): List<Comment>

    /** path 로 시작하는 모든 행 삭제(자손 전부). 삭제된 행 수 반환. */
    suspend fun deleteSubtree(postId: Long, rootPath: String): Long

    /** 게시글별 max(id) 조회. 다음 ID 생성용(PoC 단순 방식). */
    suspend fun findMaxIdByPostId(postId: Long): Long?

    /** 부모 이동: oldPrefix 로 시작하는 path 를 newPrefix 로 교체 + depth delta 적용. 영향받은 행 수 반환. */
    suspend fun updatePathPrefix(
        postId: Long,
        oldPrefix: String,
        newPrefix: String,
        depthDelta: Int
    ): Long
}
