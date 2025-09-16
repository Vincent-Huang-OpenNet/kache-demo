package com.example.member

import java.net.URI
import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/members")
class MemberController(
    private val memberService: MemberService,
) {

    @PostMapping
    suspend fun createMember(@RequestBody memberRequest: MemberRequest): ResponseEntity<Unit> =
        memberService
            .create(memberRequest)
            .let { ResponseEntity.created(URI.create("/api/members/${it.id}")).build() }

    @GetMapping
    suspend fun getAllMembers(): ResponseEntity<List<MemberResponse>> =
        memberService
            .queryAll()
            .toList()
            .let { ResponseEntity.ok(it) }

    @GetMapping("/{id}")
    suspend fun getMemberById(@PathVariable id: Long): ResponseEntity<MemberResponse> =
        memberService
            .queryById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @PutMapping("/{id}")
    suspend fun updateMember(
        @PathVariable id: Long,
        @RequestBody memberRequest: MemberRequest
    ): ResponseEntity<Unit> =
        memberService
            .modify(id, memberRequest)
            .let { ResponseEntity.noContent().build() }

    @DeleteMapping("/{id}")
    suspend fun deleteMember(@PathVariable id: Long): ResponseEntity<Unit> =
        memberService
            .removeById(id)
            .let { ResponseEntity.noContent().build() }
}
