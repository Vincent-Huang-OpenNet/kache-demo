package com.example.member

data class MemberResponse(
    val id: Long,
    val name: String,
    val email: String,
    val organizationName: String,
    val organizationEmail: String,
)
