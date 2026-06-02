package com.example.webfluxsselab.service

import com.example.webfluxsselab.model.*
import com.example.webfluxsselab.repository.*
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class RelationshipService(
    private val userRepository: UserRepository,
    private val departmentRepository: DepartmentRepository,
    private val postRepository: PostRepository,
    private val tagRepository: TagRepository,
    private val r2dbcRelationshipRepository: R2dbcRelationshipRepository
) {

    suspend fun findUserWithDepartment(userId: Long): UserWithDepartment? {
        return r2dbcRelationshipRepository.findUserWithDepartment(userId)
    }

    suspend fun findDepartmentWithManager(deptId: Long): DepartmentWithManager? {
        return r2dbcRelationshipRepository.findDepartmentWithManager(deptId)
    }

    suspend fun findPostWithAuthor(postId: Long): PostWithAuthor? {
        return r2dbcRelationshipRepository.findPostWithAuthor(postId)
    }

    fun findAllPostsWithAuthor(): Flow<PostWithAuthor> {
        return r2dbcRelationshipRepository.findAllPostsWithAuthor()
    }

    fun findByUserId(userId: Long): Flow<Post> {
        return r2dbcRelationshipRepository.findByUserId(userId)
    }

    suspend fun findTagWithPosts(tagId: Long): TagWithPosts? {
        return tagRepository.findTagWithPosts(tagId)
    }

    suspend fun findTagsByPostId(postId: Long): List<Tag> {
        return tagRepository.findTagsByPostId(postId)
    }

    suspend fun findAllPostsWithTags(): List<PostWithTags> {
        return r2dbcRelationshipRepository.findAllPostsWithTags()
    }
}
