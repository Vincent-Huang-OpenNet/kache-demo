package com.example.kaffeine

interface Kache<T> {
    /**
     * e.g. "MemberPO"
     */
    fun identifier(): String

    fun buildKey(key: String): String = "KACHE:${identifier()}:$key"

    fun buildUpdateLockKey(): String = "KACHE:UPDATE_LOCK:${identifier()}"

    /**
     * ```markdown
     * 1 If L1 cache is hit, return the value immediately
     * 2 If L1 cache missed, check L2 cache
     *   2.1 If L2 hit, update L1 cache and return value
     *   2.2 If L2 missed, acquire lock to prevent cache stampede
     *     2.2.1 If acquire lock success, update L2 cache from upstream and then refresh all L1
     *     2.2.2 If acquire lock fails (another coroutine is updating), return null immediately
     * ```
     */
    suspend fun getIfPresent(key: String): T?

    /**
     * ```markdown
     *
     * 1. update L2 cache
     * 2. update current instant L1 cache (we need this because maybe we are the only instance)
     * 3. refresh all L1 caches
     *
     * we choose double delete strategy to refresh all L1 caches since multi level cache can't be strongly consistent
     * so best practice is double delete with a small delay to achieve eventual consistency
     *
     * put("987", memberPO)
     * updateUpstream(memberPO)
     * delay(Duration.ofSeconds(3))
     * put("987", memberPO)
     * ```
     */
    suspend fun put(key: String, data: T): Boolean

    /**
     * receive signal and invalidate local L1 cache
     */
    fun invalidateLocalCache(cacheKey: String)

    /**
     * invalidate L2 cache and refresh all L1 caches
     */
    suspend fun invalidateAllCache(key: String)
}