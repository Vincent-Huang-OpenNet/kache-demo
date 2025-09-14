package com.example.member

import com.example.kaffeine.Kache
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.forEach
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

    suspend fun findAll(): Flow<MemberPO> =
        memberRepository
            .findAll()
            .also { it.collect { member -> memberCache.put(member.id.toString(), member) } }

    suspend fun findById(id: Long): MemberPO? =
        memberCache.getIfPresent(id.toString())

    suspend fun update(id: Long, memberRequest: MemberRequest): MemberPO? =
        memberRepository
            .findById(id)
            ?.copy(
                name = memberRequest.name,
                email = memberRequest.email
            )
            ?.let { entity -> memberRepository.save(entity) }

    suspend fun deleteById(id: Long): Unit = memberRepository.deleteById(id)
}
