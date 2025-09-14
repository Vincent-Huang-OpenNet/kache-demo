package com.example.kaffeine

interface LimitedKache<T>: Kache<T> {
    /*
     * refresh case usually accept dirty data, so we can just update upstream and then update cache
     */
    suspend fun refresh(key: String, asyncLoader: suspend () -> T?): Boolean
}