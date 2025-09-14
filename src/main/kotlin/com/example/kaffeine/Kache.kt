package com.example.kaffeine

interface Kache<T> {
    // e.g. "MemberPO"
    fun identifier(): String

    // e.g. key = "987", identifier = "MemberPO", buildKey returns "KACHE:MemberPO:987"
    fun buildKey(key: String): String

    // - If L1 cache is hit, return the value immediately
    // - If L1 cache missed, check L2 cache
    //   - If L2 hit, update L1 cache and return value
    //   - If L2 missed, acquire lock to prevent cache stampede
    //     - If acquire lock success, update L2 cache from upstream and then refresh all L1
    //     - If acquire lock fails (another coroutine is updating), return null immediately
    suspend fun getIfPresent(key: String): T?

    // getIfPresent(key) ?: data
    suspend fun getOrDefault(key: String, data: T): T

    // transaction {
    //   upstream.update(memberPO)
    //   put("987", memberPO)
    // }
    //
    // update L2 cache
    // update L1 cache (current instance, we need this because maybe we are the only instance)
    // refresh all L1 caches
    suspend fun put(key: String, data: T): Boolean

    // receive signal and invalidate local L1 cache
    fun invalidateLocalCache(cacheKey: String)
}