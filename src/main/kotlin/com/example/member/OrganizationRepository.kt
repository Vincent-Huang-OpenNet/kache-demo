package com.example.member

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface OrganizationRepository : CoroutineCrudRepository<OrganizationPO, Long>