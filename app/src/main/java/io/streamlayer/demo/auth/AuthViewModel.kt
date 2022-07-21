package io.streamlayer.demo.auth

import android.util.Log
import androidx.lifecycle.viewModelScope
import io.streamlayer.demo.BuildConfig
import io.streamlayer.demo.common.ext.BaseErrorEvent
import io.streamlayer.demo.common.ext.MviViewModel
import io.streamlayer.sdk.StreamLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "AuthViewModel"

data class State(
    val isLoading: Boolean = false,
    val isUserAuthorized: Boolean = false
)

class AuthViewModel : MviViewModel<State>(State(), Dispatchers.Default) {

    val isUserAuthorized = stateSlice { isUserAuthorized }
    val isLoading = stateSlice { isLoading }

    init {
        viewModelScope.launch {
            StreamLayer.userIsAuthorizedState().collect {
                updateState { copy(isUserAuthorized = it) }
            }
        }
    }

    fun bypassAuth(token: String) {
        viewModelScope.launch {
            try {
                updateState { copy(isLoading = true) }
                StreamLayer.logout()
                StreamLayer.authorizationBypass(BuildConfig.SL_BYPASS_SCHEMA, token)
                updateState { copy(isLoading = false) }
            } catch (e: Throwable) {
                Log.e(TAG, "error ${e.message}", e)
                _viewEvents.trySend(BaseErrorEvent(e.message ?: "Unknown error"))
                updateState { copy(isLoading = false) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                updateState { copy(isLoading = true) }
                StreamLayer.logout()
                updateState { copy(isLoading = false) }
            } catch (e: Throwable) {
                Log.e(TAG, "error ${e.message}", e)
                _viewEvents.trySend(BaseErrorEvent(e.message ?: "Unknown error"))
                updateState { copy(isLoading = false) }
            }
        }
    }

}