package com.example.config

import com.example.kaffeine.Kache
import com.example.kaffeine.KacheImpl
import com.example.kaffeine.KacheSynchronizer
import com.example.kaffeine.RedisPubSubSynchronizer
import com.example.member.MemberPO
import com.example.member.MemberRepository
import com.example.member.OrganizationPO
import com.example.member.OrganizationRepository
import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
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
            .recordStats()
            .build<String, MemberPO>()
            .let { caffeineCache ->
                KacheImpl(
                    identifier = "MemberPO",
                    clazz = MemberPO::class.java,
                    caffeine = caffeineCache,
                    reactiveStringRedisTemplate = reactiveStringRedisTemplate,
                    asyncUpstreamDataLoader = { key -> memberRepository.findById(key.toLong()) },
                    cacheSynchronizer = cacheSynchronizer
                )
            }

    @Bean
    fun organizationCache(
        reactiveStringRedisTemplate: ReactiveStringRedisTemplate,
        organizationRepository: OrganizationRepository,
        cacheSynchronizer: KacheSynchronizer,
    ): Kache<OrganizationPO> =
        Caffeine
            .newBuilder()
            .maximumSize(16)
            .expireAfterWrite(Duration.ofMinutes(30))
            .recordStats()
            .build<String, OrganizationPO>()
            .let { caffeineCache ->
                KacheImpl(
                    identifier = "OrganizationPO",
                    clazz = OrganizationPO::class.java,
                    caffeine = caffeineCache,
                    reactiveStringRedisTemplate = reactiveStringRedisTemplate,
                    asyncUpstreamDataLoader = { key -> organizationRepository.findById(key.toLong()) },
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
    fun registerKaches(
        kacheSynchronizer: KacheSynchronizer,
        allKacheList: List<Kache<*>>,
    ): String =
        runBlocking {
            allKacheList
                .joinToString(",") {
                    kacheSynchronizer.registerKache(it.identifier(), it)
                    it.identifier()
                }
        }
}