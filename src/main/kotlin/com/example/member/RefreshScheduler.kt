package com.example.member

import com.example.kaffeine.Kache
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class RefreshScheduler(
    private val organizationCache: Kache<OrganizationPO>,
    private val organizationRepository: OrganizationRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    @Scheduled(cron = "0 * * * * *")
    suspend fun refreshAllCaches(): Unit =
        organizationRepository
            .findAll()
            .collect { organizationPO ->
                log.info("Refreshing cache for organization: ${organizationPO.id}")
                organizationCache.refresh(organizationPO.id!!.toString())
            }
}