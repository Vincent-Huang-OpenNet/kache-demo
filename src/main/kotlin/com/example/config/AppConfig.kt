package com.example.config

import com.example.kaffeine.Kache
import com.example.kaffeine.KacheImpl
import com.example.kaffeine.KacheSynchronizer
import com.example.kaffeine.RedisPubSubSynchronizer
import com.example.member.MemberPO
import com.example.member.MemberRepository
import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveStringRedisTemplate

@Configuration
class AppConfig {
    @Bean
    fun memberCache(
        reactiveStringRedisTemplate: ReactiveStringRedisTemplate,
        memberRepository: MemberRepository,
        cacheSynchronizer: KacheSynchronizer,
    ): Kache<MemberPO> =
        Caffeine
            .newBuilder()
            .maximumSize(1024)
            .expireAfterWrite(Duration.ofMinutes(30))
            .build<String, MemberPO>()
            .let { caffeineCache ->
                KacheImpl(
                    identifier = "MemberPO",
                    clazz = MemberPO::class.java,
                    caffeine = caffeineCache,
                    reactiveStringRedisTemplate = reactiveStringRedisTemplate,
                    asyncLoader = { key -> memberRepository.findById(key.toLong()) },
                    cacheSynchronizer = cacheSynchronizer
                )
            }

    @Bean
    fun kacheSynchronizer(
        reactiveStringRedisTemplate: ReactiveStringRedisTemplate,
        reactiveRedisConnectionFactory: ReactiveRedisConnectionFactory
    ): KacheSynchronizer =
        RedisPubSubSynchronizer(
            reactiveStringRedisTemplate = reactiveStringRedisTemplate,
            reactiveRedisConnectionFactory = reactiveRedisConnectionFactory
        )

    @Bean
    fun allKacheList(
        memberCache: Kache<MemberPO>,
    ): List<Kache<*>> =
        listOf(memberCache)

    @Bean
    fun registerKachesResult(
        kacheSynchronizer: KacheSynchronizer,
        allKacheList: List<Kache<*>>,
    ): String =
        runBlocking {
            allKacheList
                .map {
                    kacheSynchronizer.registerKache(it.identifier(), it)
                    it.identifier()
                }
                .joinToString(",")
        }
}