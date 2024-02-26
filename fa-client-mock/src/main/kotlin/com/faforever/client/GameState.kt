package com.faforever.client

import com.faforever.client.event.ClientEvent
import com.faforever.gpgnet.protocol.GpgnetMessage.FromGameMessage
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

sealed interface GameState {
    fun send(message: FromGameMessage): GameState

    fun receive(message: FromGameMessage): GameState
}

class InitGameState(
    private val publishEvent: (ClientEvent) -> Unit,
    private val sendGpgnetMessage: (FromGameMessage) -> Unit,
) : GameState {
    override fun send(message: FromGameMessage): GameState {
        log.error { "The game should not send message in init state!" }
        return this
    }

    override fun receive(message: FromGameMessage): GameState {
        return IdleGameState(publishEvent, sendGpgnetMessage)
    }
}

class IdleGameState(
    private val publishEvent: (ClientEvent) -> Unit,
    private val sendGpgnetMessage: (FromGameMessage) -> Unit,
) : GameState {
    init {
        publishEvent(ClientEvent.ConnectedToGame)
    }

    override fun send(message: FromGameMessage): GameState =
        when (message) {
            else ->
                this.also {
                    log.error { "Invalid message for this state received: $message" }
                }
        }

    override fun receive(message: FromGameMessage): GameState {
        TODO("Not yet implemented")
    }
}
