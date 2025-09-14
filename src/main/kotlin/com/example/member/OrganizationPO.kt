package com.example.member

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("organization")
data class OrganizationPO(
    @Id
    val id: Long? = null,
    val name: String,
    val email: String,
)
