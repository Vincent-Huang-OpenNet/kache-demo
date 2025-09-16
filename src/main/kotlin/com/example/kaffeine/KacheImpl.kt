package com.example.kaffeine

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.benmanes.caffeine.cache.Cache
import java.time.Duration
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.getAndAwait
import org.springframework.data.redis.core.setAndAwait

class KacheImpl<T>(
    private val identifier: String,
    private val clazz: Class<T>,
    private val caffeine: Cache<String, T>,
    private val reactiveStringRedisTemplate: ReactiveStringRedisTemplate,
    private val asyncUpstreamDataLoader: suspend (key: String) -> T?,
    private val cacheSynchronizer: KacheSynchronizer? = null,
) : Kache<T> {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    override fun identifier(): String = identifier

    override suspend fun getIfPresent(key: String): T? =
        buildKey(key)
            .let { cacheKey ->
                caffeine
                    .getIfPresent(cacheKey)
                    ?.also { log.trace("Found cached value in L1 with $cacheKey") }
                    ?: reactiveStringRedisTemplate
                        .opsForValue()
                        .getAndAwait(cacheKey)
                        ?.let { objectMapper.readValue(it, clazz) }
                        ?.also { log.trace("Found cached value in L2 with $cacheKey") }
                    ?: acquireLock(buildUpdateLockKey(), Duration.ofMillis(500))
                        .takeIf { it }
                        ?.let { asyncUpstreamDataLoader(key) }
                        ?.also { log.trace("Loaded value from upstream with $cacheKey") }
                        ?.also { data -> put(key, data) }
            }

    override suspend fun put(key: String, data: T): Boolean =
        buildKey(key)
            .let { cacheKey ->
                reactiveStringRedisTemplate
                    .opsForValue()
                    .setAndAwait(cacheKey, objectMapper.writeValueAsString(data))
                    .takeIf { it }
                    ?.also { caffeine.put(cacheKey, data!!) }
                    ?.also { cacheSynchronizer?.publishCacheInvalidation(cacheKey) }
                    ?: false
            }

    override fun invalidateLocalCache(cacheKey: String): Unit =
        caffeine
            .invalidate(cacheKey)
            .also { log.trace("Local cache invalidated: $cacheKey") }

    override suspend fun invalidateAllCache(key: String) =
        buildKey(key)
            .let { cacheKey ->
                reactiveStringRedisTemplate
                    .delete(cacheKey)
                    .awaitFirst()
                    .let { caffeine.invalidate(cacheKey) }
                    .also { cacheSynchronizer?.publishCacheInvalidation(cacheKey) }
            }

    override suspend fun refresh(key: String): Boolean =
        asyncUpstreamDataLoader(key)
            ?.also { log.trace("Refreshed cache with $key") }
            ?.let { put(key, it) }
            ?: false

    private suspend fun acquireLock(key: String, duration: Duration): Boolean =
        reactiveStringRedisTemplate
            .opsForValue()
            .setIfAbsent(key, "1", duration)
            .awaitFirst()
}