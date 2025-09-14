package com.example.kaffeine

class LimitKacheImpl<T>(
    private val delegate: Kache<T>,
) : LimitedKache<T>, Kache<T> by delegate {

    override suspend fun refresh(key: String, asyncLoader: suspend () -> T?): Boolean =
        asyncLoader()
            ?.let { put(key, it) }
            ?: false
}