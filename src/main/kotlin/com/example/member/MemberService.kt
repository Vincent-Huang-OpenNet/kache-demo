package com.example.member

import com.example.kaffeine.Kache
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.time.delay
import org.springframework.stereotype.Service

@Service
class MemberService(
    private val memberCache: Kache<MemberPO>,
    private val organizationCache: Kache<OrganizationPO>,
    private val memberRepository: MemberRepository
) {

    suspend fun create(memberRequest: MemberRequest): MemberPO =
        MemberPO(
            organizationId = memberRequest.organizationId,
            name = memberRequest.name,
            email = memberRequest.email,
            createTime = Instant.now()
        )
            .let { memberRepository.save(it) }
            .also { memberCache.put(it.id.toString(), it) }

    suspend fun queryAll(): Flow<MemberResponse> =
        memberRepository
            .findAll()
            .map { memberPO ->
                val (_, name, email) = organizationCache.getOrDefault(memberPO.id!!.toString(), OrganizationPO(-1L, "", ""))

                MemberResponse(
                    id = memberPO.id,
                    name = memberPO.name,
                    email = memberPO.email,
                    organizationName = name,
                    organizationEmail = email,
                )
            }

    suspend fun queryById(id: Long): MemberResponse? =
        memberCache
            .getIfPresent(id.toString())
            ?.let { memberPO ->
                val (_, name, email) = organizationCache.getOrDefault(memberPO.id!!.toString(), OrganizationPO(-1L, "", ""))

                MemberResponse(
                    id = memberPO.id,
                    name = memberPO.name,
                    email = memberPO.email,
                    organizationName = name,
                    organizationEmail = email,
                )
            }

    suspend fun modify(id: Long, memberRequest: MemberRequest): Unit =
        run {
            val memberPO = memberRepository
                .findById(id)
                ?.copy(
                    organizationId = memberRequest.organizationId,
                    name = memberRequest.name,
                    email = memberRequest.email
                )
                ?.let { entity -> memberRepository.save(entity) }
            memberPO?.let { memberCache.put(it.id.toString(), it) }
            memberCache.invalidateAllCache(id.toString())
        }

    suspend fun removeById(id: Long): Unit =
        run {
            memberRepository.deleteById(id)
            memberCache.invalidateAllCache(id.toString())
        }
}
