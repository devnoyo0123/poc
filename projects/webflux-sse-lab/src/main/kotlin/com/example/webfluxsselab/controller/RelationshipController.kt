package com.example.webfluxsselab.controller

import com.example.webfluxsselab.model.*
import com.example.webfluxsselab.service.RelationshipService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/relationships")
class RelationshipController(
    private val relationshipService: RelationshipService
) {

    // ==================== One-to-One ====================

    @GetMapping("/users/{userId}/with-department")
    suspend fun getUserWithDepartment(@PathVariable userId: Long): UserWithDepartment? {
        return relationshipService.findUserWithDepartment(userId)
    }

    @GetMapping("/departments/{deptId}/with-manager")
    suspend fun getDepartmentWithManager(@PathVariable deptId: Long): DepartmentWithManager? {
        return relationshipService.findDepartmentWithManager(deptId)
    }

    // ==================== Many-to-One ====================

    @GetMapping("/posts/{postId}/with-author")
    suspend fun getPostWithAuthor(@PathVariable postId: Long): PostWithAuthor? {
        return relationshipService.findPostWithAuthor(postId)
    }

    @GetMapping("/posts/all-with-author")
    fun getAllPostsWithAuthor(): Flow<PostWithAuthor> {
        return relationshipService.findAllPostsWithAuthor()
    }

    // ==================== One-to-Many ====================

    @GetMapping("/users/{userId}/with-posts")
    suspend fun getUserWithPosts(@PathVariable userId: Long): UserWithPosts? {
        val posts = relationshipService.findByUserId(userId).toList()

        if (posts.isEmpty()) return null

        return UserWithPosts(
            id = userId,
            name = "User $userId",
            email = "user$userId@example.com",
            posts = posts
        )
    }

    // ==================== Many-to-Many ====================

    @GetMapping("/posts/{postId}/with-tags")
    suspend fun getPostWithTags(@PathVariable postId: Long): PostWithTags? {
        val posts = relationshipService.findByUserId(0).toList()
            .filter { it.id == postId }

        if (posts.isEmpty()) return null
        val post = posts[0]

        val tags = relationshipService.findTagsByPostId(postId)

        return PostWithTags(
            id = post.id,
            title = post.title,
            content = post.content,
            tags = tags
        )
    }

    @GetMapping("/tags/{tagId}/with-posts")
    suspend fun getTagWithPosts(@PathVariable tagId: Long): TagWithPosts? {
        return relationshipService.findTagWithPosts(tagId)
    }

    @GetMapping("/posts/all-with-tags")
    suspend fun getAllPostsWithTags(): List<PostWithTags> {
        // Two Query 방식으로 N+1 문제 해결
        return relationshipService.findAllPostsWithTags()
    }
}
