package ai.aligned.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.aligned.net.AlignedApi
import ai.aligned.net.dto.AccountDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AccountsState {
    data object Loading : AccountsState
    data class Ready(val accounts: List<AccountDto>) : AccountsState
    data class Error(val message: String) : AccountsState
}

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val api: AlignedApi
) : ViewModel() {
    private val _state = MutableStateFlow<AccountsState>(AccountsState.Loading)
    val state: StateFlow<AccountsState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            runCatching { api.accounts() }
                .onSuccess { dto ->
                    _state.value = AccountsState.Ready(
                        dto.accounts.sortedByDescending { it.followers }
                    )
                }
                .onFailure { e -> _state.value = AccountsState.Error(e.message ?: "Couldn't load") }
        }
    }
}
