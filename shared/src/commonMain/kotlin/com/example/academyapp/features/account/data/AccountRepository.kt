package com.example.academyapp.features.account.data

import com.example.academyapp.api.*
import com.example.academyapp.api.model.ProfileUpdateRequest
import com.example.academyapp.api.model.User
import com.example.academyapp.session.AcademySessionManager
import com.example.academyapp.session.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AccountRepository {
    val accountIds: Flow<Long?>
    suspend fun avatar(url: String): ByteArray?
    suspend fun profile(): ApiResult<User>
    suspend fun save(request: ProfileUpdateRequest): ApiResult<User>
    suspend fun upload(bytes: ByteArray, mimeType: String): ApiResult<User>
    suspend fun removePhoto(): ApiResult<User>
}

class AuthenticatedAccountRepository(private val api: AcademyApi, private val session: AcademySessionManager) : AccountRepository {
    override val accountIds = session.state.map { (it as? SessionState.SignedIn)?.user?.id }
    override suspend fun avatar(url: String): ByteArray? = api.getAvatarImage(url)
    override suspend fun profile() = session.executeAuthenticated { api.getMe() }
    override suspend fun save(request: ProfileUpdateRequest) = session.executeAuthenticated { api.updateProfile(request) }
    override suspend fun upload(bytes: ByteArray, mimeType: String) = session.executeAuthenticated { api.uploadAvatar(bytes, mimeType) }
    override suspend fun removePhoto() = session.executeAuthenticated { api.removeAvatar() }
}
