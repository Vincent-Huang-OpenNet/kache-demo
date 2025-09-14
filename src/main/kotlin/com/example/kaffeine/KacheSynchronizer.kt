package com.example.kaffeine

interface KacheSynchronizer {
    /**
     * use Redis Pub/Sub to notify other instances with this message "KACHE:MemberPO:987"
     */
    suspend fun publishCacheInvalidation(cacheKey: String)

    /**
     * handle local cache invalidation with this message "KACHE:MemberPO:987"
     */
    fun handleCacheInvalidation(cacheKey: String)

    /**
     * register a Kache instance to this synchronizer
     */
    fun <T> registerKache(identifier: String, kache: Kache<T>)
}