package com.example.queue.exception

sealed class QueueException(message: String) : RuntimeException(message)

class AlreadyInQueueException(userId: String) : QueueException("User $userId is already in queue")

class QueueNotFoundException(queueId: String) : QueueException("Queue $queueId not found")

class QueueFullException : QueueException("Queue is full")

class InvalidQueueStateException(message: String) : QueueException(message)
