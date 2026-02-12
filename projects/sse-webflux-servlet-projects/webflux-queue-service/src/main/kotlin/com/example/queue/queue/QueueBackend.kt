package com.example.queue.queue

import reactor.core.publisher.Mono

interface QueueBackend {
    /**
     * Add user to queue with timestamp
     * @return true if added, false if already exists
     */
    fun addToQueue(userId: String, timestamp: Long): Mono<Boolean>

    /**
     * Get user's position in queue (0-based)
     * @return position or null if not in queue
     */
    fun getPosition(userId: String): Mono<Long?>

    /**
     * Get total number of users in queue
     */
    fun getQueueSize(): Mono<Long>

    /**
     * Remove user from queue
     * @return true if removed, false if not in queue
     */
    fun removeFromQueue(userId: String): Mono<Boolean>

    /**
     * Pop next N users from queue (FIFO)
     * @return list of user IDs
     */
    fun popNext(count: Long): Mono<List<String>>

    /**
     * Set active token for user with TTL
     */
    fun setActiveToken(userId: String, token: String, ttlSeconds: Long): Mono<Boolean>

    /**
     * Get active token for user
     * @return token or null if not active
     */
    fun getActiveToken(userId: String): Mono<String?>

    /**
     * Check if user is in queue
     */
    fun isInQueue(userId: String): Mono<Boolean>
}
