package io.streamlayer.demo.managed.api

import android.util.Log
import androidx.lifecycle.viewModelScope
import io.streamlayer.demo.common.ext.BaseErrorEvent
import io.streamlayer.demo.common.ext.MviViewModel
import io.streamlayer.demo.common.ext.ViewEvent
import io.streamlayer.sdk.SLRWatchPartySession
import io.streamlayer.sdk.StreamLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

private const val TAG = "WatchPartyViewModel"

data class Participant(
    val user: SLRWatchPartySession.User,
    val status: SLRWatchPartySession.Participant.Status
)

data class Message(val userId:String, val content: String, val isLocal: Boolean)

data class WatchPartyState(
    val isActive: Boolean = false,
    val isLoading: Boolean = false,
    val participants: List<Participant> = emptyList(),
    val messages: List<Message> = emptyList()
)

data class ShowWatchParty(val watchPartySession: SLRWatchPartySession) : ViewEvent

class WatchPartyViewModel : MviViewModel<WatchPartyState>(WatchPartyState(), Dispatchers.Default) {

    private var watchPartySession: SLRWatchPartySession? = null

    private var messagesJob: Job? = null
    private var eventsJob: Job? = null
    private var subscribeJob: Job? = null

    val isActive = stateSlice { isActive }
    val isLoading = stateSlice { isLoading }
    val participants = stateSlice { participants }
    val messages = stateSlice { messages }

    private fun subToSessionUpdates() {
        catch {
            watchPartySession?.let {
                eventsJob = viewModelScope.launch {
                    it.getEvents().collect { event ->
                        Log.d(TAG, "getEvents $event")
                        when (event) {
                            is SLRWatchPartySession.Event.ParticipantsLoaded -> updateState {
                                copy(participants = event.participants.map { it.toDomain() })
                            }
                            is SLRWatchPartySession.Event.ParticipantUpdated -> {
                                updateState {
                                    val mutable = participants.toMutableList()
                                    val index = mutable.indexOfFirst { it.user == event.participant.user }
                                    if (event.isRemoved) {
                                        // remove participant from list
                                        if (index != -1) mutable.removeAt(index)
                                    } else {
                                        // add or update participant in list
                                        if (index != -1) mutable[index] = event.participant.toDomain()
                                        else mutable.add(event.participant.toDomain())
                                    }
                                    copy(participants = mutable)
                                }
                            }
                            is SLRWatchPartySession.Event.SessionStateUpdated -> {
                                _viewEvents.trySend(BaseErrorEvent("Session state is changed ${event.state}"))
                                if (event.state == SLRWatchPartySession.State.RELEASED) {
                                    unSubFromSessionUpdates()
                                    watchPartySession = null
                                }
                            }
                        }
                    }
                }
                messagesJob = viewModelScope.launch {
                    it.getMessages().collect {
                        addNewMessage(Message(it.userId, it.content, false))
                    }
                }
                updateState {
                    copy(
                        isActive = true,
                        participants = it.getParticipants().map { it.toDomain() })
                }
            } ?: kotlin.run { updateState { WatchPartyState() } }
        }
    }

    private fun unSubFromSessionUpdates() {
        eventsJob?.cancel()
        messagesJob?.cancel()
        watchPartySession?.release()
        watchPartySession = null
        updateState { copy(isActive = false, participants = emptyList(), messages = emptyList()) }
    }

    fun subscribe(groupId: String) {
        Log.d(
            TAG,
            "subscribe $groupId ${watchPartySession?.getGroupId()} ${currentState.isLoading}"
        )
        if (watchPartySession?.getGroupId() == groupId || currentState.isLoading) return
        updateState { copy(isLoading = true) }
        subscribeJob?.cancel()
        subscribeJob = viewModelScope.launch {
            watchPartySession?.let { unSubFromSessionUpdates() }
            catch(onError = { updateState { copy(isLoading = false) } }) {
                StreamLayer.createWatchPartySession(groupId).let {
                    updateState { copy(isLoading = false) }
                    watchPartySession = it
                    subToSessionUpdates()
                }
            }
        }
    }

    fun unsubscribe() {
        watchPartySession?.let { unSubFromSessionUpdates() }
        Log.d(
            TAG,
            "unsubscribe ${watchPartySession?.getGroupId()} ${currentState.isLoading}"
        )
    }

    fun openWatchParty() {
        watchPartySession?.let { _viewEvents.trySend(ShowWatchParty(watchPartySession = it)) }
    }

    fun sendMessage(content: String) {
        viewModelScope.launch {
            catch {
                watchPartySession?.let {
                    if (it.sendMessage(content)) addNewMessage(
                        Message("local", content, true)
                    )
                    else _viewEvents.trySend(BaseErrorEvent("Can not send message"))
                }
            }
        }
    }

    private fun addNewMessage(message: Message) {
        updateState {
            val messages = messages.toMutableList()
            messages.add(0, message)
            copy(messages = messages)
        }
    }

    override fun onCleared() {
        unSubFromSessionUpdates()
    }

    private inline fun catch(onError: () -> Unit = {}, block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            onError()
            Log.e(TAG, "error ${e.message}", e)
            _viewEvents.offer(BaseErrorEvent(e.message ?: "Unknown error"))
        }
    }
}

private fun SLRWatchPartySession.Participant.toDomain() = Participant(user, status)