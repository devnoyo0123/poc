package com.example.webfluxsselab.model

import java.time.LocalDateTime

// ==================== Basic Entities ====================

data class User(
    val id: Long,
    val name: String,
    val email: String
)

data class Department(
    val id: Long,
    val name: String,
    val userId: Long? = null
)

data class Post(
    val id: Long,
    val title: String,
    val content: String,
    val userId: Long,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class Tag(
    val id: Long,
    val name: String
)

data class PostTag(
    val id: Long,
    val postId: Long,
    val tagId: Long
)

// ==================== SSE Models ====================

data class Notification(
    val id: Long,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class StockPrice(
    val symbol: String,
    val price: Double,
    val change: Double,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class ChatMessage(
    val id: String,
    val user: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

// ==================== Relationship DTOs ====================

// One-to-One: User <-> Department
data class UserWithDepartment(
    val id: Long,
    val name: String,
    val email: String,
    val department: Department?
)

data class DepartmentWithManager(
    val id: Long,
    val name: String,
    val manager: User?
)

// Many-to-One: Post -> User
data class PostWithAuthor(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: LocalDateTime,
    val author: User
)

// One-to-Many: User -> Posts
data class UserWithPosts(
    val id: Long,
    val name: String,
    val email: String,
    val posts: List<Post>
)

// Many-to-Many: Post <-> Tags
data class PostWithTags(
    val id: Long,
    val title: String,
    val content: String,
    val tags: List<Tag>
)

data class TagWithPosts(
    val id: Long,
    val name: String,
    val posts: List<Post>
)
