package com.faforever.client.event

sealed interface ClientEvent {
    data object IdleGameState : ClientEvent

    data object LobbyGameState : ClientEvent
}

abstract class ClientEventListener(
    private val filter: ((ClientEvent) -> Boolean) = { true },
) {
    internal fun onEventUnfiltered(clientEvent: ClientEvent) {
        if (filter(clientEvent)) {
            onEvent(clientEvent)
        }
    }

    abstract fun onEvent(clientEvent: ClientEvent)
}
