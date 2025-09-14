package com.example.kaffeine

interface Kache<T> {
    fun buildKey(key: String): String

    suspend fun getIfPresent(key: String): T?

    suspend fun getOrDefault(key: String, data: T): T

    suspend fun put(key: String, data: T)

    fun existLocal(key: String): Boolean

    fun invalidateLocalCache(cacheKey: String)
}