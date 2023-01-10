package io.streamlayer.demo.managed.api

import android.util.Log
import androidx.lifecycle.viewModelScope
import io.streamlayer.demo.common.ext.BaseErrorEvent
import io.streamlayer.demo.common.ext.MviViewModel
import io.streamlayer.demo.common.ext.ViewEvent
import io.streamlayer.sdk.SLRManagedGroupSession
import io.streamlayer.sdk.StreamLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TAG = "ManagedGroupViewModel"

data class Participant(
    val userId: String,
    val bypassId: String,
    val status: SLRManagedGroupSession.Participant.Status
)

data class Message(val userId: String, val content: String, val isLocal: Boolean)

data class WatchPartyState(
    val isActive: Boolean = false,
    val isLoading: Boolean = false,
    val participants: List<Participant> = emptyList(),
    val messages: List<Message> = emptyList()
)

data class ShowWatchParty(val session: SLRManagedGroupSession) : ViewEvent
data class ShowChat(val session: SLRManagedGroupSession) : ViewEvent

class ManagedGroupViewModel : MviViewModel<WatchPartyState>(WatchPartyState(), Dispatchers.Default) {

    private var managedGroupSession: SLRManagedGroupSession? = null

    private var messagesJob: Job? = null
    private var eventsJob: Job? = null
    private var subscribeJob: Job? = null

    val isActive = stateSlice { isActive }
    val isLoading = stateSlice { isLoading }
    val participants = stateSlice { participants }
    val messages = stateSlice { messages }

    private fun subToSessionUpdates() {
        catch {
            managedGroupSession?.let {
                eventsJob = viewModelScope.launch {
                    it.getEvents().collect { event ->
                        Log.d(TAG, "getEvents $event")
                        when (event) {
                            is SLRManagedGroupSession.Event.ParticipantsLoaded -> updateState {
                                copy(participants = event.participants.map { it.toDomain() })
                            }
                            is SLRManagedGroupSession.Event.ParticipantUpdated -> {
                                updateState {
                                    val mutable = participants.toMutableList()
                                    val index =
                                        mutable.indexOfFirst { it.userId == event.participant.userId }
                                    if (event.isRemoved) {
                                        // remove participant from list
                                        if (index != -1) mutable.removeAt(index)
                                    } else {
                                        // add or update participant in list
                                        if (index != -1) mutable[index] =
                                            event.participant.toDomain()
                                        else mutable.add(event.participant.toDomain())
                                    }
                                    copy(participants = mutable)
                                }
                            }
                            is SLRManagedGroupSession.Event.SessionStateUpdated -> {
                                _viewEvents.trySend(BaseErrorEvent("Session state is changed ${event.state}"))
                                if (event.state == SLRManagedGroupSession.State.RELEASED) {
                                    unSubFromSessionUpdates()
                                    managedGroupSession = null
                                }
                            }
                        }
                    }
                }
                messagesJob = viewModelScope.launch {
                    it.getInfoMessages().collect {
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
        managedGroupSession?.release()
        managedGroupSession = null
        updateState { copy(isActive = false, participants = emptyList(), messages = emptyList()) }
    }

    fun subscribe(groupId: String) {
        Log.d(
            TAG,
            "subscribe $groupId ${managedGroupSession?.getGroupId()} ${currentState.isLoading}"
        )
        if (managedGroupSession?.getGroupId() == groupId || currentState.isLoading) return
        updateState { copy(isLoading = true) }
        subscribeJob?.cancel()
        subscribeJob = viewModelScope.launch {
            managedGroupSession?.let { unSubFromSessionUpdates() }
            catch(onError = { updateState { copy(isLoading = false) } }) {
                StreamLayer.createManagedGroupSession(groupId, null).let {
                    updateState { copy(isLoading = false) }
                    managedGroupSession = it
                    subToSessionUpdates()
                }
            }
        }
    }

    fun unsubscribe() {
        managedGroupSession?.let { unSubFromSessionUpdates() }
        Log.d(
            TAG,
            "unsubscribe ${managedGroupSession?.getGroupId()} ${currentState.isLoading}"
        )
    }

    fun openWatchParty() {
        managedGroupSession?.let { _viewEvents.trySend(ShowWatchParty(session = it)) }
    }

    fun openChat() {
        managedGroupSession?.let { _viewEvents.trySend(ShowChat(session = it)) }
    }

    fun sendMessage(content: String) {
        viewModelScope.launch {
            catch {
                managedGroupSession?.let {
                    if (it.sendInfoMessage(content)) addNewMessage(
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
            _viewEvents.trySend(BaseErrorEvent(e.message ?: "Unknown error"))
        }
    }
}

private fun SLRManagedGroupSession.Participant.toDomain() = Participant(userId, bypassId, status)