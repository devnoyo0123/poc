package com.example.queue.dto

import com.example.queue.domain.Priority
import jakarta.validation.constraints.NotBlank

data class JoinQueueRequest(
    @field:NotBlank(message = "User ID is required")
    val userId: String,
    val priority: Priority = Priority.NORMAL,
    val metadata: Map<String, Any> = emptyMap()
)
