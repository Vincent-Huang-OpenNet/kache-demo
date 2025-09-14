package com.example.kaffeine

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking

class KacheRegistrar(
    private val kacheSynchronizer: KacheSynchronizer,
    private val kaches: List<Kache<*>>,
) {
    @PostConstruct
    fun registerCaches() =
        runBlocking {
            kaches.forEach { kacheSynchronizer.registerKache(it.identifier(), it) }
        }
}