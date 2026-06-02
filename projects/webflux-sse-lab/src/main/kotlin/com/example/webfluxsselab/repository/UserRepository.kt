package com.example.webfluxsselab.repository

import com.example.webfluxsselab.model.*
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : R2dbcRepository<User, Long> {

    @Query("""
        SELECT u.id as user_id, u.name as user_name, u.email as user_email,
               d.id as dept_id, d.name as dept_name, d.user_id as dept_user_id
        FROM users u
        LEFT JOIN departments d ON u.id = d.user_id
        WHERE u.id = :userId
    """)
    suspend fun findUserWithDepartment(userId: Long): UserWithDepartment?
}

@Repository
interface DepartmentRepository : R2dbcRepository<Department, Long> {

    @Query("""
        SELECT d.id as dept_id, d.name as dept_name, d.user_id as dept_user_id,
               u.id as user_id, u.name as user_name, u.email as user_email
        FROM departments d
        LEFT JOIN users u ON d.user_id = u.id
        WHERE d.id = :deptId
    """)
    suspend fun findDepartmentWithManager(deptId: Long): DepartmentWithManager?
}

@Repository
interface PostRepository : R2dbcRepository<Post, Long> {

    @Query("""
        SELECT p.id as post_id, p.title, p.content, p.created_at,
               u.id as author_id, u.name as author_name, u.email as author_email
        FROM posts p
        INNER JOIN users u ON p.user_id = u.id
        WHERE p.id = :postId
    """)
    suspend fun findPostWithAuthor(postId: Long): PostWithAuthor?

    @Query("""
        SELECT p.id as post_id, p.title, p.content, p.created_at,
               u.id as author_id, u.name as author_name, u.email as author_email
        FROM posts p
        INNER JOIN users u ON p.user_id = u.id
        ORDER BY p.created_at DESC
    """)
    fun findAllPostsWithAuthor(): Flow<PostWithAuthor>

    @Query("SELECT * FROM posts WHERE user_id = :userId ORDER BY created_at DESC")
    fun findByUserId(userId: Long): Flow<Post>
}

@Repository
interface TagRepository : R2dbcRepository<Tag, Long> {

    @Query("""
        SELECT t.id as tag_id, t.name as tag_name,
               p.id as post_id, p.title, p.content
        FROM tags t
        LEFT JOIN post_tags pt ON t.id = pt.tag_id
        LEFT JOIN posts p ON pt.post_id = p.id
        WHERE t.id = :tagId
        ORDER BY p.created_at DESC
    """)
    suspend fun findTagWithPosts(tagId: Long): TagWithPosts?

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN post_tags pt ON t.id = pt.tag_id
        WHERE pt.post_id = :postId
    """)
    suspend fun findTagsByPostId(postId: Long): List<Tag>
}
