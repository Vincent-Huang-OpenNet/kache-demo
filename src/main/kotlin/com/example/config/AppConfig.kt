package com.example.config

import com.example.kaffeine.Kache
import com.example.kaffeine.KacheImpl
import com.example.kaffeine.KacheRegistrar
import com.example.kaffeine.KacheSynchronizer
import com.example.kaffeine.RedisPubSubSynchronizer
import com.example.member.MemberPO
import com.example.member.MemberRepository
import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration
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
    ): Kache<MemberPO> {
        val caffeineCache = Caffeine
            .newBuilder()
            .maximumSize(1024)
            .expireAfterWrite(Duration.ofMinutes(30))
            .build<String, MemberPO>()

        return KacheImpl(
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
    ): KacheSynchronizer {
        return RedisPubSubSynchronizer(
            reactiveStringRedisTemplate,
            reactiveRedisConnectionFactory
        )
    }

    @Bean
    fun allKacheList(
        memberCache: Kache<MemberPO>,
    ): List<Kache<*>> =
        listOf(memberCache)

    @Bean
    fun kacheRegister(
        kacheSynchronizer: KacheSynchronizer,
        allKacheList: List<Kache<*>>,
    ): KacheRegistrar {
        return KacheRegistrar(kacheSynchronizer, allKacheList)
    }
}