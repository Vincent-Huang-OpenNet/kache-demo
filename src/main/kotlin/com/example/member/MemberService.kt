package com.example.member

import com.example.kaffeine.Kache
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.time.delay
import org.springframework.stereotype.Service

@Service
class MemberService(
    private val memberCache: Kache<MemberPO>,
    private val memberRepository: MemberRepository
) {

    suspend fun create(memberRequest: MemberRequest): MemberPO =
        MemberPO(
            name = memberRequest.name,
            email = memberRequest.email,
            createTime = Instant.now()
        )
            .let { memberRepository.save(it) }
            .also { memberCache.put(it.id.toString(), it) }

    suspend fun queryAll(): Flow<MemberPO> = memberRepository.findAll()

    suspend fun queryById(id: Long): MemberPO? = memberCache.getIfPresent(id.toString())

    suspend fun modify(id: Long, memberRequest: MemberRequest): Unit =
        run {
            memberCache.invalidateAllCache(id.toString())
            val memberPO = memberRepository
                .findById(id)
                ?.copy(
                    name = memberRequest.name,
                    email = memberRequest.email
                )
                ?.let { entity -> memberRepository.save(entity) }
            delay(Duration.ofSeconds(3000))
            memberPO?.let { memberCache.put(it.id.toString(), it) }
            memberCache.invalidateAllCache(id.toString())
        }

    suspend fun removeById(id: Long): Unit =
        run {
            memberCache.invalidateAllCache(id.toString())
            memberRepository.deleteById(id)
            delay(Duration.ofSeconds(3000))
            memberCache.invalidateAllCache(id.toString())
        }
}
