package com.example.kaffeine

interface KacheSynchronizer {
    suspend fun publishCacheInvalidation(cacheKey: String)

    suspend fun <T> registerCache(identifier: String, kache: Kache<T>)
}