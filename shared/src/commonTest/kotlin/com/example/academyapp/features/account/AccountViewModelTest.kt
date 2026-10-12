package com.example.academyapp.features.account

import com.example.academyapp.api.*
import com.example.academyapp.api.model.*
import com.example.academyapp.features.account.data.AccountRepository
import com.example.academyapp.features.account.presentation.*
import com.example.academyapp.features.account.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    private val user = User(1, "learner@example.test", "Ana García", "ana", "Ana", "García", "+541155551234")
    private inner class Repository : AccountRepository {
        override val accountIds = MutableStateFlow<Long?>(1)
        var value = user
        var failure: AcademyApiError? = null
        var saves = 0
        var uploads = 0
        var removals = 0
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun avatar(url: String): ByteArray? = null
        override suspend fun profile(): ApiResult<User> { gate?.await(); return failure?.let { ApiResult.Failure(it) } ?: ApiResult.Success(value) }
        override suspend fun save(request: ProfileUpdateRequest): ApiResult<User> {
            saves++; gate?.await()
            failure?.let { return ApiResult.Failure(it) }
            value = value.copy(firstName = request.firstName, surname = request.surname, phone = request.phone)
            return ApiResult.Success(value)
        }
        override suspend fun upload(bytes: ByteArray, mimeType: String): ApiResult<User> { uploads++; failure?.let { return ApiResult.Failure(it) }; value = value.copy(avatarUrl = "https://fixture.invalid/avatar.jpg"); return ApiResult.Success(value) }
        override suspend fun removePhoto(): ApiResult<User> { removals++; value = value.copy(avatarUrl = null); return ApiResult.Success(value) }
    }

    @Test fun loadEditValidateSaveReloadAndReadOnlyIdentity() = runTest {
        val repo = Repository(); val vm = AccountViewModel(repo, backgroundScope)
        runCurrent(); vm.open(1); runCurrent()
        assertEquals(user, vm.state.value.user)
        vm.edit("firstName", ""); vm.save(); runCurrent()
        assertEquals("required", vm.state.value.fieldErrors["firstName"]); assertEquals(0, repo.saves)
        vm.edit("firstName", "  Ana   María "); vm.edit("phone", "+54 (11) 5555-1234"); vm.save(); runCurrent()
        assertEquals("Ana María", vm.state.value.firstName); assertFalse(vm.state.value.dirty); assertTrue(vm.state.value.saved)
        assertEquals(user.email, vm.state.value.user?.email); assertEquals(user.username, vm.state.value.user?.username)
        vm.reload(); runCurrent(); assertEquals("Ana María", vm.state.value.firstName)
    }
    @Test fun loadFailureCanRetryAndIncompleteLegacyFieldsRemainEditable() = runTest {
        val repo = Repository(); repo.failure = AcademyApiError.Server(503, null)
        val vm = AccountViewModel(repo, backgroundScope); runCurrent(); vm.open(1); runCurrent()
        assertTrue(vm.state.value.failed); assertNull(vm.state.value.user); assertFalse(vm.state.value.busy)
        repo.failure = null; repo.value = user.copy(firstName = "", surname = "", phone = "")
        vm.reload(); runCurrent(); assertNotNull(vm.state.value.user)
        vm.edit("firstName", "Ana"); vm.edit("surname", "García"); vm.edit("phone", "123456789"); vm.save(); runCurrent()
        assertTrue(vm.state.value.saved)
    }
    @Test fun nameAndPhoneLimitsAndUnicode() {
        assertTrue(validateProfile(ProfileUpdateRequest("José", "Núñez", "+541155551234")).isEmpty())
        assertEquals(3, validateProfile(ProfileUpdateRequest("<html>", "x".repeat(101), "123")).size)
        assertEquals(1, validateProfile(ProfileUpdateRequest("Ana", "García", "+" + "1".repeat(16))).size)
    }
    @Test fun failedSaveRetainsDraftAndMapsFieldErrors() = runTest {
        val repo = Repository(); val vm = AccountViewModel(repo, backgroundScope)
        runCurrent(); vm.open(1); runCurrent(); vm.edit("firstName", "New")
        repo.failure = AcademyApiError.BadRequest(ApiError("invalid_profile", "", ApiErrorData(400, mapOf("firstName" to "invalid_name"))))
        vm.save(); runCurrent()
        assertTrue(vm.state.value.dirty); assertEquals("New", vm.state.value.firstName); assertTrue(vm.state.value.failed)
        assertEquals("invalid_name", vm.state.value.fieldErrors["firstName"])
        repo.failure = null; vm.save(); runCurrent(); assertTrue(vm.state.value.saved)
    }
    @Test fun photoCancelFailureReplaceAndRemovePreserveTextDraft() = runTest {
        val repo = Repository(); val vm = AccountViewModel(repo, backgroundScope)
        runCurrent(); vm.open(1); runCurrent(); vm.edit("firstName", "Draft")
        vm.selected(PhotoSelection.Cancelled); assertEquals(0, repo.uploads)
        vm.selected(PhotoSelection.Selected(ByteArray(MAX_PHOTO_BYTES + 1), "image/jpeg")); assertEquals(0, repo.uploads)
        vm.selected(PhotoSelection.Selected(byteArrayOf(1), "image/png")); runCurrent()
        assertEquals("Draft", vm.state.value.firstName); assertNotNull(vm.state.value.user?.avatarUrl)
        repo.failure = AcademyApiError.Server(500, null)
        vm.selected(PhotoSelection.Selected(byteArrayOf(1), "image/png")); runCurrent()
        assertTrue(vm.state.value.photoError); assertNotNull(vm.state.value.user?.avatarUrl)
        vm.removePhoto(); runCurrent(); assertNull(vm.state.value.user?.avatarUrl); assertEquals("Draft", vm.state.value.firstName)
    }
    @Test fun discardGuardAndAccountSwitchClearPrivateState() = runTest {
        val repo = Repository(); val vm = AccountViewModel(repo, backgroundScope)
        runCurrent(); vm.open(1); runCurrent(); vm.edit("surname", "Draft")
        var left = false; vm.requestLeave { left = true }
        assertTrue(vm.state.value.confirmDiscard); assertFalse(left)
        vm.cancelLeave(); assertFalse(left); assertTrue(vm.state.value.dirty)
        vm.requestLeave { left = true }; vm.confirmLeave(); assertTrue(left); assertFalse(vm.state.value.dirty)
        vm.edit("firstName", "Private draft"); repo.accountIds.value = null; runCurrent()
        assertEquals(AccountUiState(), vm.state.value)
        repo.accountIds.value = 2; repo.value = user.copy(id = 2, firstName = "Other"); runCurrent(); vm.open(2); runCurrent()
        assertEquals("Other", vm.state.value.firstName)
    }
    @Test fun busyBlocksDuplicateWritesAndLogoutClearsPendingLoad() = runTest {
        val repo = Repository(); val vm = AccountViewModel(repo, backgroundScope)
        runCurrent(); vm.open(1); runCurrent(); vm.edit("firstName", "New")
        repo.gate = CompletableDeferred(); vm.save(); vm.save(); runCurrent()
        assertTrue(vm.state.value.busy); assertEquals(1, repo.saves)
        repo.accountIds.value = null; runCurrent(); repo.gate?.complete(Unit); runCurrent()
        assertEquals(AccountUiState(), vm.state.value)
    }
}
