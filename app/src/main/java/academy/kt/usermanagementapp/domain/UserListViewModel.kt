package academy.kt.usermanagementapp.domain

import academy.kt.usermanagementapp.data.network.UserRepository
import academy.kt.usermanagementapp.model.AddUser
import academy.kt.usermanagementapp.model.User
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _state: MutableStateFlow<UserListState> = MutableStateFlow(UserListState.Loading)
    val state: StateFlow<UserListState> = _state

    private val _error: MutableStateFlow<Throwable?> = MutableStateFlow(null)
    val error: StateFlow<Throwable?> = _error

    init {
        loadUsers()
    }

    fun removeUser(id: Int) {
        viewModelScope.launch {
            userRepository.removeUser(id)
                .onSuccess { loadUsers() }
                .onFailure { _error.value = it }
        }
    }

    fun addUser(addUser: AddUser) {
        viewModelScope.launch {
            userRepository.addUser(addUser)
                .onSuccess { loadUsers() }
                .onFailure { _error.value = it }
        }
    }

    fun refresh() {
        _state.value = UserListState.Loading
        loadUsers()
    }

    fun hideError() {
        _error.value = null
    }

    private fun loadUsers() {
        viewModelScope.launch {
            userRepository.fetchUsers()
                .onSuccess { _state.value = UserListState.ShowList(it) }
                .onFailure {
                    _state.value = UserListState.ShowList(emptyList())
                    _error.value = it
                }
        }
    }
}

sealed interface UserListState {
    object Loading : UserListState
    data class ShowList(val users: List<User>) : UserListState
}