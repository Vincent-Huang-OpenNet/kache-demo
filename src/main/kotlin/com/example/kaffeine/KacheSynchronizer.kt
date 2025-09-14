package com.example.kaffeine

interface KacheSynchronizer {
    suspend fun publishCacheInvalidation(cacheKey: String)

    suspend fun <T> registerKache(identifier: String, kache: Kache<T>)
}