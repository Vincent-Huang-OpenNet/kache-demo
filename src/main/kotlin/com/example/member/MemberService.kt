package com.example.member

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class MemberService(private val memberRepository: MemberRepository) {

    suspend fun create(memberRequest: MemberRequest): MemberPO {
        val member = MemberPO(
            name = memberRequest.name,
            email = memberRequest.email,
            createTime = Instant.now()
        )
        return memberRepository.save(member)
    }

    suspend fun findAll(): Flow<MemberPO> = memberRepository.findAll()

    suspend fun findById(id: Long): MemberPO? = memberRepository.findById(id)

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
