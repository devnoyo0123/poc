package com.career.pathenum.service

import com.career.pathenum.exception.CommentNotFoundException
import com.career.pathenum.exception.InvalidDepthException
import com.career.pathenum.exception.ParentNotFoundException
import com.career.pathenum.exception.PostMismatchException
import com.career.pathenum.model.Comment
import com.career.pathenum.model.dto.CommentResponse
import com.career.pathenum.model.dto.CommentTreeNode
import com.career.pathenum.model.dto.CreateCommentRequest
import com.career.pathenum.repository.CommentRepository
import com.career.pathenum.util.Base62Encoder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.time.LocalDateTime

/**
 * 댓글 도메인 서비스.
 *
 * Path Enumeration 의 핵심 연산:
 *   - createComment  : path/depth 계산 후 저장
 *   - getCommentTree : ORDER BY path → 메모리 DFS 빌드
 *   - getDescendants : LIKE '<path>%' 자손 조회
 *   - getAncestors   : path 세그먼트 IN 조회
 *   - deleteComment  : LIKE prefix 일괄 삭제
 *   - moveComment    : prefix 교체 + depth delta 일괄 UPDATE
 *
 * 트랜잭션: "조회-후-쓰기" 패턴(create/delete/move)만 TransactionalOperator 로 감싼다.
 * ID 생성: PoC 단순화로 게시글별 max(id)+1 사용. 실무는 Snowflake / sequence 테이블 권장.
 */
@Service
class CommentService(
    private val repo: CommentRepository,
    private val transactionalOperator: TransactionalOperator
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 댓글 생성.
     *   - parent_id == null → 루트: path = encode(id), depth = 1
     *   - parent_id != null → 답글: path = parent.path + encode(id), depth = parent.depth + 1
     */
    suspend fun createComment(req: CreateCommentRequest): Comment =
        transactionalOperator.executeAndAwait {
            val id = (repo.findMaxIdByPostId(req.postId) ?: 0L) + 1

            val (path, depth, parentId) = if (req.parentId == null) {
                Triple(Base62Encoder.encode(id), 1, null)
            } else {
                val parent = repo.findById(req.parentId)
                    ?: throw ParentNotFoundException(req.parentId)
                if (parent.postId != req.postId) {
                    throw PostMismatchException(
                        "parent postId=${parent.postId} != request postId=${req.postId}"
                    )
                }
                Triple(parent.path + Base62Encoder.encode(id), parent.depth + 1, req.parentId)
            }

            val comment = Comment(
                id = id,
                postId = req.postId,
                parentId = parentId,
                path = path,
                depth = depth,
                content = req.content,
                createdAt = LocalDateTime.now(),
                updatedAt = null
            )
            log.info(
                "createComment id={} postId={} parentId={} path={} depth={}",
                id, req.postId, parentId, path, depth
            )
            repo.save(comment)
        } ?: throw IllegalStateException("transaction failed: createComment")

    suspend fun getComment(id: Long): Comment =
        repo.findById(id) ?: throw CommentNotFoundException(id)

    /**
     * 게시글 전체 댓글을 DFS 트리로 빌드.
     *
     * 전략: path 오름차순 정렬(utf8mb4_bin → ASCII 순서 = DFS 순서)된 flat list 에서
     * "가장 마지막에 본 depth-1 노드"를 현재 노드의 부모로 결정.
     */
    suspend fun getCommentTree(postId: Long): List<CommentTreeNode> {
        val all = repo.findByPostIdOrderByPath(postId)
        if (all.isEmpty()) return emptyList()

        // 작업용 mutable 노드들(외부 응답은 immutable CommentTreeNode 로 변환)
        data class BuildNode(
            val response: CommentResponse,
            val children: MutableList<BuildNode> = mutableListOf()
        )

        fun toTreeNode(n: BuildNode): CommentTreeNode = CommentTreeNode(
            comment = n.response,
            children = n.children.map(::toTreeNode)
        )

        val roots = mutableListOf<BuildNode>()
        val lastAtDepth = HashMap<Int, BuildNode>()

        for (c in all) {
            val response = CommentResponse(
                id = c.id,
                postId = c.postId,
                parentId = c.parentId,
                path = c.path,
                depth = c.depth,
                content = c.content,
                createdAt = c.createdAt.toString(),
                updatedAt = c.updatedAt?.toString()
            )
            val node = BuildNode(response)
            val depth = c.depth

            if (depth == 1) {
                roots.add(node)
            } else {
                val parent = lastAtDepth[depth - 1]
                if (parent == null) {
                    log.warn("orphan node id={} depth={} - promoting to root", c.id, depth)
                    roots.add(node)
                } else {
                    parent.children.add(node)
                }
            }
            lastAtDepth[depth] = node
            // 더 깊은 depth 의 마지막 정보는 더 이상 부모 후보가 아님 → 제거
            lastAtDepth.keys.filter { it > depth }.forEach(lastAtDepth::remove)
        }
        return roots.map(::toTreeNode)
    }

    /** 자신을 제외한 모든 자손. */
    suspend fun getDescendants(commentId: Long): List<Comment> {
        val c = repo.findById(commentId) ?: throw CommentNotFoundException(commentId)
        return repo.findSubtree(c.postId, c.path).filter { it.id != commentId }
    }

    /** 루트까지의 조상 전체(자신 제외). */
    suspend fun getAncestors(commentId: Long): List<Comment> {
        val c = repo.findById(commentId) ?: throw CommentNotFoundException(commentId)
        return repo.findAncestors(c.postId, c.path)
    }

    /**
     * 자손 전부 일괄 삭제. 자기 자신도 LIKE 'selfPath%' 에 포함돼 함께 삭제됨.
     */
    suspend fun deleteComment(commentId: Long): Long =
        transactionalOperator.executeAndAwait {
            val c = repo.findById(commentId) ?: throw CommentNotFoundException(commentId)
            repo.deleteSubtree(c.postId, c.path)
        } ?: 0L

    /**
     * 부모 이동: 자신 + 모든 자손의 path prefix 갱신. 영향받은 행 수 반환.
     *
     * 순환 방지: newParent.path.startsWith(comment.path) → 자손으로 이동 불가.
     * depth delta = (newParent?.depth ?: 0) + 1 - comment.depth.
     */
    suspend fun moveComment(commentId: Long, newParentId: Long?): Long =
        transactionalOperator.executeAndAwait {
            val comment = repo.findById(commentId) ?: throw CommentNotFoundException(commentId)

            val (newParentPath, newDepth) = if (newParentId == null) {
                Pair(null, 1)
            } else {
                val newParent = repo.findById(newParentId)
                    ?: throw ParentNotFoundException(newParentId)
                if (newParent.postId != comment.postId) {
                    throw PostMismatchException(
                        "newParent postId=${newParent.postId} != comment postId=${comment.postId}"
                    )
                }
                if (newParent.id == comment.id || newParent.path.startsWith(comment.path)) {
                    throw InvalidDepthException(
                        "cannot move comment $commentId to its own descendant or itself"
                    )
                }
                Pair(newParent.path, newParent.depth + 1)
            }

            val newPath = (newParentPath ?: "") + Base62Encoder.encode(comment.id)
            val depthDelta = newDepth - comment.depth
            repo.updatePathPrefix(comment.postId, comment.path, newPath, depthDelta)
        } ?: 0L
}
