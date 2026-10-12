package com.example.academyapp.features.account.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.academyapp.api.*
import com.example.academyapp.api.model.*
import com.example.academyapp.features.account.data.AccountRepository
import com.example.academyapp.features.account.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Drafts and image bytes are memory-only and cleared when the authenticated account changes. */
data class AccountUiState(
    val user: User? = null,
    val firstName: String = "",
    val surname: String = "",
    val phone: String = "",
    val busy: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val failed: Boolean = false,
    val saved: Boolean = false,
    val photoError: Boolean = false,
    val avatarBytes: ByteArray? = null,
    val confirmDiscard: Boolean = false,
) {
    val dirty: Boolean get() = user != null && (firstName != user.firstName || surname != user.surname || phone != user.phone)
}
class AccountViewModel(private val repository: AccountRepository, coroutineScope: CoroutineScope? = null) : ViewModel() {
    private val scope = coroutineScope ?: viewModelScope
    private val mutable = MutableStateFlow(AccountUiState())
    val state = mutable.asStateFlow()
    private var pendingLeave: (() -> Unit)? = null
    private var owner: Long? = null
    private var generation = 0L
    private var operation: Job? = null

    init {
        scope.launch {
            repository.accountIds.distinctUntilChanged().collect { id ->
                if (owner != id) {
                    generation++
                    operation?.cancel()
                    pendingLeave = null
                    owner = id
                    mutable.value = AccountUiState()
                }
            }
        }
    }

    fun open(id: Long) {
        if (owner != id) { generation++; operation?.cancel(); pendingLeave = null; owner = id; mutable.value = AccountUiState() }
        if (state.value.user == null && !state.value.busy) reload()
    }

    fun edit(field: String, value: String) {
        if (state.value.busy) return
        mutable.value = when (field) {
            "firstName" -> state.value.copy(firstName = value)
            "surname" -> state.value.copy(surname = value)
            "phone" -> state.value.copy(phone = value)
            else -> return
        }.copy(fieldErrors = state.value.fieldErrors - field, saved = false, failed = false)
    }

    fun requestLeave(action: () -> Unit) {
        if (state.value.busy) return
        if (state.value.dirty) { pendingLeave = action; mutable.value = state.value.copy(confirmDiscard = true) }
        else action()
    }
    fun cancelLeave() { pendingLeave = null; mutable.value = state.value.copy(confirmDiscard = false) }
    fun confirmLeave() { val action = pendingLeave; pendingLeave = null; discard(); action?.invoke() }
    fun discard() { state.value.user?.let { mutable.value = loaded(it, state.value.avatarBytes) } }
    fun reload() = perform { repository.profile() }

    fun save() {
        val draft = state.value
        val request = ProfileUpdateRequest(normalizeName(draft.firstName), normalizeName(draft.surname), normalizePhone(draft.phone))
        val errors = validateProfile(request)
        if (errors.isNotEmpty()) { mutable.value = draft.copy(fieldErrors = errors, saved = false); return }
        perform(saved = true) { repository.save(request) }
    }

    fun selected(result: PhotoSelection) {
        when (result) {
            PhotoSelection.Cancelled -> Unit
            PhotoSelection.Failed -> mutable.value = state.value.copy(photoError = true, saved = false)
            is PhotoSelection.Selected -> {
                if (result.bytes.isEmpty() || result.bytes.size > MAX_PHOTO_BYTES || result.mimeType !in listOf("image/jpeg", "image/png")) {
                    mutable.value = state.value.copy(photoError = true, saved = false)
                } else perform(saved = true, keepDraft = true) { repository.upload(result.bytes, result.mimeType) }
            }
        }
    }
    fun removePhoto() = perform(saved = true, keepDraft = true) { repository.removePhoto() }

    private fun perform(saved: Boolean = false, keepDraft: Boolean = false, request: suspend () -> ApiResult<User>) {
        if (state.value.busy || owner == null) return
        val id = owner
        val version = generation
        val previous = state.value
        mutable.value = previous.copy(busy = true, failed = false, photoError = false, saved = false, fieldErrors = emptyMap())
        operation = scope.launch {
            try {
                when (val result = request()) {
                    is ApiResult.Success -> if (owner == id && generation == version && result.value.id == id) {
                        val image = result.value.avatarUrl?.let { repository.avatar(it) }
                        if (owner != id || generation != version) return@launch
                        mutable.value = loaded(result.value, image).let {
                            if (keepDraft) it.copy(firstName = previous.firstName, surname = previous.surname, phone = previous.phone, saved = saved)
                            else it.copy(saved = saved)
                        }
                    } else if (owner == id && generation == version) mutable.value = previous.copy(failed = true)
                    is ApiResult.Failure -> if (owner == id && generation == version) {
                        val errors = (result.error as? AcademyApiError.BadRequest)?.details?.data?.fieldErrors.orEmpty()
                        mutable.value = previous.copy(failed = true, fieldErrors = errors, saved = false, photoError = keepDraft)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                if (owner == id && generation == version) mutable.value = previous.copy(failed = true, photoError = keepDraft, saved = false)
            } finally {
                if (owner == id && generation == version) mutable.value = state.value.copy(busy = false)
            }
        }
    }

    private fun loaded(user: User, image: ByteArray? = null) = AccountUiState(user, user.firstName, user.surname, user.phone, avatarBytes = image)
}
