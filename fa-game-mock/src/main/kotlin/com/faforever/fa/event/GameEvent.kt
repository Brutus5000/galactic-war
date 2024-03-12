package com.faforever.fa.event

import com.faforever.fa.GameLobby

sealed interface GameEvent {
    data class LobbyOpened(val gameLobby: GameLobby) : GameEvent

    data object DataSent : GameEvent

    data class DataReceived(val bytes: ByteArray) : GameEvent
}

abstract class GameEventListener(
    private val filter: ((GameEvent) -> Boolean) = { true },
) {
    internal fun onEventUnfiltered(gameEvent: GameEvent) {
        if (filter(gameEvent)) {
            onEvent(gameEvent)
        }
    }

    abstract fun onEvent(gameEvent: GameEvent)
}

class OnEventHandler<T : GameEvent>(val type: Class<T>, val callback: (T) -> Unit) : GameEventListener(filter = { it.javaClass == type }) {
    override fun onEvent(gameEvent: GameEvent) {
        callback(gameEvent as T)
    }
}
