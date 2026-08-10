package com.example.redis.apiserver.controller

import com.example.redis.apiserver.service.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class RedisController(
    private val stringCacheService: StringCacheService,
    private val hashCacheService: HashCacheService,
    private val leaderboardService: LeaderboardService,
    private val distributedLockService: DistributedLockService,
    private val pipelineService: PipelineService,
    private val pubSubService: PubSubService
) {

    // ========== String Cache ==========

    @PostMapping("/string/product/{id}")
    fun saveProductString(@PathVariable id: String, @RequestBody product: Map<String, Any>) =
        stringCacheService.saveProduct(id, product)

    @GetMapping("/string/product/{id}")
    fun getProductString(@PathVariable id: String) =
        stringCacheService.getProduct(id) ?: mapOf("error" to "Not found")

    @DeleteMapping("/string/product/{id}")
    fun deleteProductString(@PathVariable id: String) =
        stringCacheService.deleteProduct(id)

    @GetMapping("/string/product/{id}/ttl")
    fun getProductTTL(@PathVariable id: String) =
        mapOf("ttl" to stringCacheService.getTTL(id))

    @PostMapping("/string/product/{id}/decrease-stock")
    fun decreaseStockString(@PathVariable id: String, @RequestParam qty: Int = 1) =
        stringCacheService.decreaseStockUnsafe(id, qty)

    // ========== Hash Cache ==========

    @PostMapping("/hash/product/{id}")
    fun saveProductHash(
        @PathVariable id: String,
        @RequestParam name: String,
        @RequestParam price: Int,
        @RequestParam stock: Int
    ) = hashCacheService.saveProduct(id, name, price, stock)

    @GetMapping("/hash/product/{id}")
    fun getProductHash(@PathVariable id: String) =
        hashCacheService.getProduct(id)

    @GetMapping("/hash/product/{id}/{field}")
    fun getProductField(@PathVariable id: String, @PathVariable field: String) =
        mapOf(field to hashCacheService.getField(id, field))

    @PostMapping("/hash/product/{id}/decrease-stock")
    fun decreaseStockHash(@PathVariable id: String, @RequestParam qty: Int = 1) =
        hashCacheService.decreaseStock(id, qty)

    @PostMapping("/hash/product/{id}/update-name")
    fun updateProductName(@PathVariable id: String, @RequestParam name: String) =
        hashCacheService.updateName(id, name)

    @PostMapping("/hash/counter/{apiName}")
    fun incrementCounter(@PathVariable apiName: String) =
        mapOf("api" to apiName, "count" to hashCacheService.incrementApiCounter(apiName))

    @GetMapping("/hash/counters")
    fun getAllCounters() = hashCacheService.getAllCounters()

    @GetMapping("/hash/concurrency-test/{id}")
    fun concurrencyTest(@PathVariable id: String, @RequestParam iterations: Int = 100) =
        hashCacheService.concurrencyTest(id, iterations)

    // ========== Sorted Set (Leaderboard) ==========

    @PostMapping("/leaderboard/add")
    fun addScore(@RequestParam playerId: String, @RequestParam score: Double) =
        leaderboardService.addScore(playerId, score)

    @PostMapping("/leaderboard/increment")
    fun incrementScore(@RequestParam playerId: String, @RequestParam delta: Double) =
        mapOf("playerId" to playerId, "newScore" to leaderboardService.incrementScore(playerId, delta))

    @GetMapping("/leaderboard/top")
    fun getTopN(@RequestParam(defaultValue = "10") n: Long) =
        leaderboardService.getTopN(n)

    @GetMapping("/leaderboard/rank/{playerId}")
    fun getMyRank(@PathVariable playerId: String) =
        leaderboardService.getMyRank(playerId)

    @GetMapping("/leaderboard/count")
    fun getTotalPlayers() =
        mapOf("totalPlayers" to leaderboardService.getTotalPlayers())

    @GetMapping("/leaderboard/range")
    fun getByScoreRange(@RequestParam min: Double, @RequestParam max: Double) =
        leaderboardService.getByScoreRange(min, max)

    @DeleteMapping("/leaderboard")
    fun clearLeaderboard() = leaderboardService.clear()

    // ========== Distributed Lock ==========

    @PostMapping("/lock/acquire")
    fun acquireLock(@RequestParam lockKey: String, @RequestParam(defaultValue = "10") expireSeconds: Long) =
        distributedLockService.acquireLock(lockKey, expireSeconds)

    @PostMapping("/lock/release")
    fun releaseLock(@RequestParam lockKey: String, @RequestParam lockValue: String) =
        distributedLockService.releaseLock(lockKey, lockValue)

    @PostMapping("/lock/release-unsafe")
    fun releaseLockUnsafe(@RequestParam lockKey: String, @RequestParam lockValue: String) =
        distributedLockService.releaseLockUnsafe(lockKey, lockValue)

    @PostMapping("/lock/product/{id}/decrease-stock")
    fun decreaseStockWithLock(@PathVariable id: String, @RequestParam qty: Int = 1) =
        distributedLockService.decreaseStockWithLock(id, qty)

    // ========== Pipeline ==========

    @PostMapping("/pipeline/individual")
    fun setIndividual(@RequestParam(defaultValue = "100") count: Int) =
        pipelineService.setIndividually(count)

    @PostMapping("/pipeline/batch")
    fun setWithPipeline(@RequestParam(defaultValue = "100") count: Int) =
        pipelineService.setWithPipeline(count)

    @PostMapping("/pipeline/get-multiple")
    fun getMultiple(@RequestBody keys: List<String>) =
        pipelineService.getMultipleWithPipeline(keys)

    // ========== Pub/Sub (범용) ==========

    @PostMapping("/pubsub/publish")
    fun publish(@RequestParam channel: String, @RequestParam message: String) =
        pubSubService.publish(channel, message)

    @PostMapping("/pubsub/subscribe")
    fun subscribe(@RequestParam channel: String) =
        pubSubService.subscribe(channel)

    @GetMapping("/pubsub/history")
    fun getPubSubHistory() =
        mapOf("messages" to pubSubService.getMessageHistory())

    @DeleteMapping("/pubsub/history")
    fun clearPubSubHistory() =
        mapOf("status" to pubSubService.clearHistory())
}
