package com.example.academyapp.session

import com.example.academyapp.api.AccessTokenProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MutableAccessTokenProvider : AccessTokenProvider {
    private val mutex = Mutex()
    private var token: String? = null

    override suspend fun accessToken(): String? = mutex.withLock { token }

    suspend fun update(accessToken: String?) {
        mutex.withLock {
            token = accessToken?.takeIf(String::isNotBlank)
        }
    }
}
