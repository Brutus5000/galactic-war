package com.faforever.client

import com.faforever.client.event.ClientEvent
import com.faforever.gpgnet.protocol.GameStateEnum
import com.faforever.gpgnet.protocol.GameStateMessage
import com.faforever.gpgnet.protocol.GpgnetMessage.FromGameMessage
import com.faforever.gpgnet.protocol.GpgnetMessage.ToGameMessage
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

sealed interface GameState {
    fun send(message: ToGameMessage): GameState

    fun receive(message: FromGameMessage): GameState
}

class InitGameState(
    private val publishEvent: (ClientEvent) -> Unit,
    private val sendGpgnetMessage: (ToGameMessage) -> Unit,
) : GameState {
    override fun send(message: ToGameMessage): GameState {
        log.error { "The game should not send message in init state!" }
        return this
    }

    override fun receive(message: FromGameMessage): GameState =
        when (message) {
            is GameStateMessage -> {
                check(message.gameState == GameStateEnum.IDLE)
                IdleGameState(publishEvent, sendGpgnetMessage)
            }

            else -> {
                log.error { "Invalid message for this state received: $message" }
                this
            }
        }
}

class IdleGameState(
    private val publishEvent: (ClientEvent) -> Unit,
    private val sendGpgnetMessage: (ToGameMessage) -> Unit,
) : GameState {
    init {
        publishEvent(ClientEvent.IdleGameState)
    }

    override fun send(message: ToGameMessage): GameState {
        sendGpgnetMessage(message)
        return this
    }

    override fun receive(message: FromGameMessage): GameState =
        when (message) {
            is GameStateMessage -> {
                check(message.gameState == GameStateEnum.LOBBY)
                LobbyGameState(publishEvent, sendGpgnetMessage)
            }

            else -> {
                log.error { "Invalid message for this state received: $message" }
                this
            }
        }
}

class LobbyGameState(
    private val publishEvent: (ClientEvent) -> Unit,
    private val sendGpgnetMessage: (ToGameMessage) -> Unit,
) : GameState {
    init {
        publishEvent(ClientEvent.LobbyGameState)
    }

    override fun send(message: ToGameMessage): GameState {
        sendGpgnetMessage(message)
        return this
    }

    override fun receive(message: FromGameMessage): GameState =
        when (message) {
            is GameStateMessage -> {
                check(message.gameState == GameStateEnum.LOBBY)
                IdleGameState(publishEvent, sendGpgnetMessage)
            }

            else -> {
                log.error { "Invalid message for this state received: $message" }
                this
            }
        }
}
