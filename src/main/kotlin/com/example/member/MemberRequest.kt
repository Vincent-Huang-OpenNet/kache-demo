package com.example.member

data class MemberRequest(
    val organizationId: Long,
    val name: String,
    val email: String
)
