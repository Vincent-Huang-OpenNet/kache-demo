package com.example.kaffeine

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.benmanes.caffeine.cache.Cache
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.getAndAwait
import org.springframework.data.redis.core.setAndAwait

class KacheImpl<T>(
    private val identifier: String,
    private val clazz: Class<T>,
    private val caffeine: Cache<String, T>,
    private val reactiveStringRedisTemplate: ReactiveStringRedisTemplate,
    private val asyncLoader: suspend (key: String) -> T?,
    private val cacheSynchronizer: KacheSynchronizer? = null,
) : Kache<T> {
    private val objectMapper = jacksonObjectMapper()

    override fun buildKey(key: String): String = "kache:$identifier:$key"

    override suspend fun getIfPresent(key: String): T? {
        val cacheKey = buildKey(key)

        val l1CachedValue = caffeine.getIfPresent(cacheKey)
        if (l1CachedValue != null) {
            return l1CachedValue
        }

        val l2Cache: String? = reactiveStringRedisTemplate.opsForValue().getAndAwait(cacheKey)
        val l2CachedValue: T? = l2Cache?.let { objectMapper.readValue(it, clazz) }

        if (l2CachedValue != null) {
            caffeine.put(cacheKey, l2CachedValue)
            return l2CachedValue
        }

        val upstreamData = asyncLoader(key)
        if (upstreamData != null) {
            this.put(key, upstreamData)
            return upstreamData
        } else {
            return null
        }
    }

    override suspend fun getOrDefault(key: String, data: T): T = getIfPresent(key) ?: data

    override suspend fun put(key: String, data: T) {
        val cacheKey = buildKey(key)

        reactiveStringRedisTemplate.opsForValue().setAndAwait(cacheKey, objectMapper.writeValueAsString(data))

        caffeine.put(cacheKey, data)

        cacheSynchronizer?.publishCacheInvalidation(cacheKey)
    }

    override fun existLocal(key: String): Boolean = caffeine.getIfPresent(key) != null

    override fun invalidateLocalCache(cacheKey: String) = caffeine.invalidate(cacheKey)
}