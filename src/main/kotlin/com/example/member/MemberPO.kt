package com.example.member

import java.time.Instant
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("member")
data class MemberPO(
    @Id
    val id: Long? = null,
    val name: String,
    val email: String,
    val createTime: Instant
)
