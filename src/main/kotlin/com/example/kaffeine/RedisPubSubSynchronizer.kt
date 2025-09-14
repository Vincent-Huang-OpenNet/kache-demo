package com.example.kaffeine

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer

class RedisPubSubSynchronizer(
    private val reactiveStringRedisTemplate: ReactiveStringRedisTemplate,
    private val reactiveRedisConnectionFactory: ReactiveRedisConnectionFactory
) : KacheSynchronizer {
    companion object {
        private const val CACHE_INVALIDATION_CHANNEL = "kache:invalidation"
    }

    private val objectMapper = jacksonObjectMapper()
    private val registeredCaches = ConcurrentHashMap<String, Kache<*>>()
    private lateinit var messageListener: ReactiveRedisMessageListenerContainer

    @PostConstruct
    fun initialize() {
        messageListener = ReactiveRedisMessageListenerContainer(reactiveRedisConnectionFactory)

        messageListener.receive(ChannelTopic(CACHE_INVALIDATION_CHANNEL))
            .doOnNext { topicMessage ->
                val invalidationMessage = objectMapper.readValue(
                    topicMessage.message,
                    CacheInvalidationMessage::class.java
                )

                handleCacheInvalidation(invalidationMessage)
            }
            .subscribe()
    }

    @PreDestroy
    fun destroy() {
        if (::messageListener.isInitialized) {
            messageListener.destroyLater().subscribe()
        }
    }

    override suspend fun publishCacheInvalidation(cacheKey: String) {
        reactiveStringRedisTemplate
            .convertAndSend(
                CACHE_INVALIDATION_CHANNEL,
                objectMapper.writeValueAsString(CacheInvalidationMessage(cacheKey))
            )
            .awaitFirst()
    }

    override suspend fun <T> registerCache(identifier: String, kache: Kache<T>) {
        registeredCaches[identifier] = kache
    }

    private fun handleCacheInvalidation(message: CacheInvalidationMessage) {
        registeredCaches
            .entries
            .firstOrNull { identifier -> message.cacheKey.split(":")[1] == identifier.key }
            ?.value
            ?.invalidateLocalCache(message.cacheKey)
    }

    data class CacheInvalidationMessage(
        val cacheKey: String
    )
}