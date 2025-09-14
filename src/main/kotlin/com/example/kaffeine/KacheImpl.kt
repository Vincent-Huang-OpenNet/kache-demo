package com.example.kaffeine

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.benmanes.caffeine.cache.Cache
import org.slf4j.LoggerFactory
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
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
        private const val KEY_PREFIX = "KACHE"
    }

    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    override fun identifier(): String = identifier

    override fun buildKey(key: String): String = "$KEY_PREFIX:$identifier:$key"

    override suspend fun getIfPresent(key: String): T? =
        buildKey(key)
            .let { cacheKey ->
                caffeine
                    .getIfPresent(cacheKey)
                    ?.also { log.debug("Found cached value in L1 with $cacheKey") }
                    ?: reactiveStringRedisTemplate
                        .opsForValue()
                        .getAndAwait(cacheKey)
                        ?.let { objectMapper.readValue(it, clazz) }
                        ?.also { log.debug("Found cached value in L2 with $cacheKey") }
                        ?: asyncLoader(key)
                            ?.also { log.debug("Loaded value from upstream with $cacheKey") }
                            ?.also { data -> put(key, data) }
            }

    override suspend fun getOrDefault(key: String, data: T): T = getIfPresent(key) ?: data

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
            .also { log.debug("Local cache invalidated: $cacheKey") }
}