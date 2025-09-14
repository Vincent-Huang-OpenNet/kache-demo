package com.example.member

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface MemberRepository : CoroutineCrudRepository<MemberPO, Long>