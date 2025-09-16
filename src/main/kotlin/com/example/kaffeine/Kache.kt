package com.example.kaffeine

/**
 * TODO: Write the document about how kache works to prevent these issues: Cache Avalanche, Cache Breakdown, and Cache Penetration
 */
interface Kache<T> {
    /**
     * For example, "MemberPO"
     *
     * 1. We use this to classify different caches for registering to KacheSynchronizer
     * 2. We use this to build a unique cache key prefix
     */
    fun identifier(): String

    /**
     * For example, "KACHE:MemberPO:987"
     *
     * 1. We use this as the unique cache key in both local and distributed caches
     */
    fun buildKey(key: String): String = "KACHE:${identifier()}:$key"

    /**
     * For example "KACHE:UPDATE_LOCK:MemberPO:987"
     *
     * 1. We use this as the distributed lock key in prevent cache breakdown progress
     */
    fun buildUpdateLockKey(): String = "KACHE:UPDATE_LOCK:${identifier()}"

    /**
     * ```markdown
     * Check local cache
     *     Local cache is hit, return the value immediately
     *     Local cache missed, check distributed cache
     *         Distributed cache is hit, update local cache and return value
     *         Distributed cache missed, acquire distributed lock
     *             Acquire distributed lock success, check upstream data source
     *                 Upstream data source has data, update distributed cache and then refresh all local caches
     *                 Upstream data source has no data, return null
     *             Acquire distributed lock failure, return null
     * ```
     */
    suspend fun getIfPresent(key: String): T?

    /**
     * Sometimes we can accept a default value for a while until the cache is refreshed
     */
    suspend fun getOrDefault(key: String, data: T): T = getIfPresent(key) ?: data

    /**
     * ```markdown
     * update distributed cache
     * update local cache on current instance (we need this because maybe we are the only instance)
     * broadcast to refresh all local caches
     * ```
     */
    suspend fun put(key: String, data: T): Boolean

    /**
     * Receive signal and invalidate the local cache
     */
    fun invalidateLocalCache(cacheKey: String)

    /**
     * Invalidate distributed cache and broadcast to refresh all local caches
     */
    suspend fun invalidateAllCache(key: String)

    /**
     * According to the key, reload data from upstream and update both distributed and broadcast to refresh all local caches
     */
    suspend fun refresh(key: String): Boolean
}