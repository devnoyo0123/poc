package com.example.webfluxsselab.repository

import com.example.webfluxsselab.jooq.tables.Departments
import com.example.webfluxsselab.jooq.tables.PostTags
import com.example.webfluxsselab.jooq.tables.Posts
import com.example.webfluxsselab.jooq.tables.Tags
import com.example.webfluxsselab.jooq.tables.Users
import com.example.webfluxsselab.model.*
import io.r2dbc.spi.Row
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.jooq.DSLContext
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class R2dbcRelationshipRepository(
    private val dsl: DSLContext,
    private val client: DatabaseClient
) {

    // ==================== User with Department ====================

    suspend fun findUserWithDepartment(userId: Long): UserWithDepartment? {
        val u = Users.USERS
        val d = Departments.DEPARTMENTS

        val query = dsl.select(
            u.ID,
            u.NAME,
            u.EMAIL,
            d.ID.`as`("dept_id"),
            d.NAME.`as`("dept_name"),
            d.USER_ID.`as`("dept_user_id")
        )
            .from(u)
            .leftJoin(d)
            .on(u.ID.eq(d.USER_ID))
            .where(u.ID.eq(userId))

        val sql = dsl.renderInlined(query)

        return client.sql(sql)
            .map { row, _ -> mapRowToUserWithDepartment(row) }
            .one()
            .awaitSingleOrNull()
    }

    private fun mapRowToUserWithDepartment(row: Row): UserWithDepartment {
        val userId = row.get("id")
        val userName = row.get("name")
        val userEmail = row.get("email")
        val deptId = row.get("dept_id")
        val deptName = row.get("dept_name")
        val deptUserId = row.get("dept_user_id")

        return UserWithDepartment(
            id = when (userId) {
                is Number -> userId.toLong()
                is String -> userId.toLong()
                else -> throw IllegalArgumentException("Cannot convert userId: $userId")
            },
            name = userName as String,
            email = userEmail as String,
            department = if (deptId != null) Department(
                id = when (deptId) {
                    is Number -> deptId.toLong()
                    is String -> deptId.toLong()
                    else -> throw IllegalArgumentException("Cannot convert deptId: $deptId")
                },
                name = deptName as String,
                userId = when (deptUserId) {
                    null -> null
                    is Number -> deptUserId.toLong()
                    is String -> deptUserId.toLong()
                    else -> throw IllegalArgumentException("Cannot convert deptUserId: $deptUserId")
                }
            ) else null
        )
    }

    // ==================== Post with Author ====================

    suspend fun findPostWithAuthor(postId: Long): PostWithAuthor? {
        val p = Posts.POSTS
        val u = Users.USERS

        val query = dsl.select(
            p.ID,
            p.TITLE,
            p.CONTENT,
            p.CREATED_AT,
            u.ID.`as`("author_id"),
            u.NAME.`as`("author_name"),
            u.EMAIL.`as`("author_email")
        )
            .from(p)
            .innerJoin(u)
            .on(p.USER_ID.eq(u.ID))
            .where(p.ID.eq(postId))

        val sql = dsl.renderInlined(query)

        return client.sql(sql)
            .map { row, _ -> mapRowToPostWithAuthor(row) }
            .one()
            .awaitSingleOrNull()
    }

    private fun mapRowToPostWithAuthor(row: Row): PostWithAuthor {
        val postId = row.get("id")
        val postTitle = row.get("title")
        val postContent = row.get("content")
        val postCreatedAt = row.get("created_at")
        val authorId = row.get("author_id")
        val authorName = row.get("author_name")
        val authorEmail = row.get("author_email")

        return PostWithAuthor(
            id = when (postId) {
                is Number -> postId.toLong()
                else -> throw IllegalArgumentException("Cannot convert postId: $postId")
            },
            title = postTitle as String,
            content = postContent as String,
            createdAt = when (postCreatedAt) {
                is LocalDateTime -> postCreatedAt
                is java.sql.Timestamp -> (postCreatedAt as java.sql.Timestamp).toLocalDateTime()
                else -> throw IllegalArgumentException("Cannot convert createdAt: $postCreatedAt")
            },
            author = User(
                id = when (authorId) {
                    is Number -> authorId.toLong()
                    else -> throw IllegalArgumentException("Cannot convert authorId: $authorId")
                },
                name = authorName as String,
                email = authorEmail as String
            )
        )
    }

    fun findAllPostsWithAuthor(): Flow<PostWithAuthor> {
        val p = Posts.POSTS
        val u = Users.USERS

        val query = dsl.select(
            p.ID,
            p.TITLE,
            p.CONTENT,
            p.CREATED_AT,
            u.ID.`as`("author_id"),
            u.NAME.`as`("author_name"),
            u.EMAIL.`as`("author_email")
        )
            .from(p)
            .innerJoin(u)
            .on(p.USER_ID.eq(u.ID))
            .orderBy(p.CREATED_AT.desc())

        val sql = dsl.renderInlined(query)

        return client.sql(sql)
            .map { row, _ -> mapRowToPostWithAuthor(row) }
            .flow()
    }

    // ==================== User Posts ====================

    // ==================== Department with Manager ====================

    suspend fun findDepartmentWithManager(deptId: Long): DepartmentWithManager? {
        val d = Departments.DEPARTMENTS
        val u = Users.USERS

        val query = dsl.select(
            d.ID,
            d.NAME,
            u.ID.`as`("manager_id"),
            u.NAME.`as`("manager_name"),
            u.EMAIL.`as`("manager_email")
        )
            .from(d)
            .leftJoin(u)
            .on(d.USER_ID.eq(u.ID))
            .where(d.ID.eq(deptId))

        val sql = dsl.renderInlined(query)

        return client.sql(sql)
            .map { row, _ -> mapRowToDepartmentWithManager(row) }
            .one()
            .awaitSingleOrNull()
    }

    private fun mapRowToDepartmentWithManager(row: Row): DepartmentWithManager {
        val deptId = row.get("id")
        val deptName = row.get("name")
        val managerId = row.get("manager_id")
        val managerName = row.get("manager_name")
        val managerEmail = row.get("manager_email")

        return DepartmentWithManager(
            id = when (deptId) {
                is Number -> deptId.toLong()
                else -> throw IllegalArgumentException("Cannot convert deptId: $deptId")
            },
            name = deptName as String,
            manager = if (managerId != null) User(
                id = when (managerId) {
                    is Number -> managerId.toLong()
                    else -> throw IllegalArgumentException("Cannot convert managerId: $managerId")
                },
                name = managerName as String,
                email = managerEmail as String
            ) else null
        )
    }

    fun findByUserId(userId: Long): Flow<Post> {
        val p = Posts.POSTS

        val query = dsl.select(
            p.ID,
            p.TITLE,
            p.CONTENT,
            p.USER_ID,
            p.CREATED_AT
        )
            .from(p)
            .where(p.USER_ID.eq(userId))
            .orderBy(p.CREATED_AT.desc())

        val sql = dsl.renderInlined(query)

        return client.sql(sql)
            .map { row, _ ->
                val id = row.get("id")
                val userId = row.get("user_id")
                Post(
                    id = when (id) {
                        is Number -> id.toLong()
                        else -> throw IllegalArgumentException("Cannot convert id: $id")
                    },
                    title = row.get("title") as String,
                    content = row.get("content") as String,
                    userId = when (userId) {
                        is Number -> userId.toLong()
                        else -> throw IllegalArgumentException("Cannot convert userId: $userId")
                    },
                    createdAt = when (val createdAt = row.get("created_at")) {
                        is LocalDateTime -> createdAt
                        is java.sql.Timestamp -> createdAt.toLocalDateTime()
                        else -> throw IllegalArgumentException("Cannot convert createdAt: $createdAt")
                    }
                )
            }
            .flow()
    }

    // ==================== Posts with Tags (Two Query - N+1 해결) ====================

    suspend fun findAllPostsWithTags(): List<PostWithTags> {
        val p = Posts.POSTS
        val t = Tags.TAGS
        val pt = PostTags.POST_TAGS

        // (1) 전체 포스트 조회
        val postsQuery = dsl.select(
            p.ID,
            p.TITLE,
            p.CONTENT,
            p.USER_ID,
            p.CREATED_AT
        )
            .from(p)
            .orderBy(p.CREATED_AT.desc())

        val postsSql = dsl.renderInlined(postsQuery)
        val postList = client.sql(postsSql)
            .map { row, _ ->
                Post(
                    id = when (val id = row.get("id")) {
                        is Number -> id.toLong()
                        else -> throw IllegalArgumentException("Cannot convert id: $id")
                    },
                    title = row.get("title") as String,
                    content = row.get("content") as String,
                    userId = when (val userId = row.get("user_id")) {
                        is Number -> userId.toLong()
                        else -> throw IllegalArgumentException("Cannot convert userId: $userId")
                    },
                    createdAt = when (val createdAt = row.get("created_at")) {
                        is LocalDateTime -> createdAt
                        is java.sql.Timestamp -> createdAt.toLocalDateTime()
                        else -> throw IllegalArgumentException("Cannot convert createdAt: $createdAt")
                    }
                )
            }
            .flow()
            .toList()

        if (postList.isEmpty()) return emptyList()

        // (2) 한 번에 모든 태그 조회 (WHERE IN)
        val postIds = postList.map { post: Post -> post.id }
        val tagsQuery = dsl.select(
            pt.POST_ID.`as`("post_id"),
            t.ID.`as`("tag_id"),
            t.NAME.`as`("tag_name")
        )
            .from(t)
            .innerJoin(pt).on(t.ID.eq(pt.TAG_ID))
            .where(pt.POST_ID.`in`(postIds))
            .orderBy(pt.POST_ID, t.ID)

        val tagsSql = dsl.renderInlined(tagsQuery)
        val tagList = client.sql(tagsSql)
            .map { row, _ ->
                val postId = when (val postId = row.get("post_id")) {
                    is Number -> postId.toLong()
                    else -> throw IllegalArgumentException("Cannot convert postId: $postId")
                }
                val tagId = when (val tagId = row.get("tag_id")) {
                    is Number -> tagId.toLong()
                    else -> throw IllegalArgumentException("Cannot convert tagId: $tagId")
                }
                val tagName = row.get("tag_name") as String

                Triple(postId, tagId, tagName)
            }
            .flow()
            .toList()

        // 태그 맵 생성 (postId -> List<Tag>)
        val tagMap: Map<Long, List<Tag>> = tagList
            .groupBy { (postId, _, _) -> postId }
            .mapValues { (_, triples) ->
                triples.map { (_, tagId, tagName) ->
                    Tag(id = tagId, name = tagName)
                }
            }

        // (3) 조립
        return postList.map { post: Post ->
            PostWithTags(
                id = post.id,
                title = post.title,
                content = post.content,
                tags = tagMap[post.id] ?: emptyList()
            )
        }
    }
}
