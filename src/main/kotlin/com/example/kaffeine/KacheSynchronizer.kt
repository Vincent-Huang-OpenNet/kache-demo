package com.example.kaffeine

interface KacheSynchronizer {
    /**
     * For example, use Redis Pub/Sub to notify other instances with this message "KACHE:MemberPO:987"
     */
    suspend fun publishCacheInvalidation(cacheKey: String)

    /**
     * For example, handle local cache invalidation with this message "KACHE:MemberPO:987"
     */
    fun handleCacheInvalidation(cacheKey: String)

    /**
     * register a Kache instance with identifier to this synchronizer
     */
    fun <T> registerKache(identifier: String, kache: Kache<T>)
}