package com.career.pathenum.repository

import com.career.pathenum.generated.tables.records.CommentsRecord
import com.career.pathenum.generated.tables.references.COMMENTS
import com.career.pathenum.model.Comment
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.impl.DSL
import org.jooq.impl.DSL.concat
import org.jooq.impl.DSL.`val`
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

private val DSLMaxIdField: Field<Long?> = DSL.field("max({0})", Long::class.java, COMMENTS.ID)

private fun DSLSubstringFrom(field: Field<String?>, start: Int): Field<String?> =
    DSL.field("substring({0} from {1})", String::class.java, field, DSL.`val`(start))

private fun DSLNow(): Field<LocalDateTime> = DSL.field("now()", LocalDateTime::class.java)

@Repository
class CommentRepositoryImpl(
    private val dsl: DSLContext
) : CommentRepository {

    private val log = LoggerFactory.getLogger(javaClass)
    private val c = COMMENTS

    override suspend fun save(comment: Comment): Comment {
        val insert = dsl.insertInto(c)
            .set(c.ID, comment.id)
            .set(c.POST_ID, comment.postId)
            .set(c.PARENT_ID, comment.parentId)
            .set(c.PATH, comment.path)
            .set(c.DEPTH, comment.depth)
            .set(c.CONTENT, comment.content)
            .set(c.CREATED_AT, comment.createdAt)
            .apply {
                if (comment.updatedAt != null) {
                    set(c.UPDATED_AT, comment.updatedAt)
                }
            }

        val rows = insert.awaitSingle()
        log.debug("save rows={} id={}", rows, comment.id)
        return comment
    }

    override suspend fun findById(id: Long): Comment? =
        dsl.selectFrom(c)
            .where(c.ID.eq(id))
            .awaitFirstOrNull()
            ?.let(::recordToComment)

    override suspend fun findByPostIdOrderByPath(postId: Long): List<Comment> =
        dsl.selectFrom(c)
            .where(c.POST_ID.eq(postId))
            .orderBy(c.PATH.asc())
            .asFlow()
            .toList()
            .map(::recordToComment)

    override suspend fun findSubtree(postId: Long, rootPath: String): List<Comment> {
        val likePattern = escapeLike(rootPath) + "%"
        return dsl.selectFrom(c)
            .where(c.POST_ID.eq(postId))
            .and(c.PATH.like(likePattern))
            .orderBy(c.PATH.asc())
            .asFlow()
            .toList()
            .map(::recordToComment)
    }

    override suspend fun findDirectChildren(
        postId: Long,
        parentPath: String,
        parentDepth: Int
    ): List<Comment> {
        val likePattern = escapeLike(parentPath) + "%"
        return dsl.selectFrom(c)
            .where(c.POST_ID.eq(postId))
            .and(c.PATH.like(likePattern))
            .and(c.DEPTH.eq(parentDepth + 1))
            .orderBy(c.PATH.asc())
            .asFlow()
            .toList()
            .map(::recordToComment)
    }

    override suspend fun findAncestors(postId: Long, path: String): List<Comment> {
        if (path.length < 10) return emptyList()

        val ancestorPaths = (5 until path.length step 5).map { end ->
            path.substring(0, end)
        }

        return dsl.selectFrom(c)
            .where(c.POST_ID.eq(postId))
            .and(c.PATH.`in`(ancestorPaths))
            .orderBy(c.PATH.asc())
            .asFlow()
            .toList()
            .map(::recordToComment)
    }

    override suspend fun deleteSubtree(postId: Long, rootPath: String): Long {
        val likePattern = escapeLike(rootPath) + "%"
        val rows = dsl.deleteFrom(c)
            .where(c.POST_ID.eq(postId))
            .and(c.PATH.like(likePattern))
            .awaitSingle()
        log.info("deleteSubtree postId={} rootPath={} deleted={}", postId, rootPath, rows)
        return rows.toLong()
    }

    override suspend fun findMaxIdByPostId(postId: Long): Long? =
        dsl.select(DSL.coalesce(DSL.max(c.ID), DSL.`val`(0L)))
            .from(c)
            .where(c.POST_ID.eq(postId))
            .awaitSingle()
            .value1()

    override suspend fun updatePathPrefix(
        postId: Long,
        oldPrefix: String,
        newPrefix: String,
        depthDelta: Int
    ): Long {
        val likePattern = escapeLike(oldPrefix) + "%"
        val startPos = oldPrefix.length + 1

        val rows = dsl.update(c)
            .set(
                c.PATH,
                concat(`val`(newPrefix), DSLSubstringFrom(c.PATH, startPos))
            )
            .set(c.DEPTH, c.DEPTH.plus(depthDelta))
            .set(c.UPDATED_AT, DSLNow())
            .where(c.POST_ID.eq(postId))
            .and(c.PATH.like(likePattern))
            .awaitSingle()
        log.info(
            "updatePathPrefix postId={} old={} new={} delta={} affected={}",
            postId, oldPrefix, newPrefix, depthDelta, rows
        )
        return rows.toLong()
    }

    private fun recordToComment(r: CommentsRecord): Comment = Comment(
        id = r.id ?: error("required column null: id"),
        postId = r.postId ?: error("required column null: post_id"),
        parentId = r.parentId,
        path = r.path ?: error("required column null: path"),
        depth = r.depth ?: error("required column null: depth"),
        content = r.content ?: error("required column null: content"),
        createdAt = r.createdAt ?: error("required column null: created_at"),
        updatedAt = r.updatedAt
    )

    private fun escapeLike(s: String): String = s
        .replace("\\", "\\\\")
        .replace("_", "\\_")
        .replace("%", "\\%")
}
