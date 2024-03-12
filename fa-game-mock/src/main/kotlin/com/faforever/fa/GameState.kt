package com.faforever.fa

import com.faforever.fa.GameLobby.PlayerId
import com.faforever.gpgnet.protocol.ConnectToPeerMessage
import com.faforever.gpgnet.protocol.CreateLobbyMessage
import com.faforever.gpgnet.protocol.DisconnectFromPeerMessage
import com.faforever.gpgnet.protocol.GameStateEnum
import com.faforever.gpgnet.protocol.GameStateMessage
import com.faforever.gpgnet.protocol.GpgnetMessage.FromGameMessage
import com.faforever.gpgnet.protocol.GpgnetMessage.ToGameMessage
import com.faforever.gpgnet.protocol.HostGameMessage
import com.faforever.gpgnet.protocol.JoinGameMessage
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

sealed interface GameState {
    fun process(message: ToGameMessage): GameState
}

class IdleGameState(
    val sendGpgnetMessage: (FromGameMessage) -> Unit,
) : GameState {
    init {
        sendGpgnetMessage(GameStateMessage(GameStateEnum.IDLE))
    }

    override fun process(message: ToGameMessage): GameState =
        when (message) {
            is CreateLobbyMessage ->
                LobbyGameState(
                    sendGpgnetMessage = sendGpgnetMessage,
                    lobby =
                        GameLobby(
                            lobbyInitMode = message.lobbyInitMode,
                            lobbyPort = message.lobbyPort,
                            localPlayerName = message.localPlayerName,
                            localPlayerId = message.localPlayerId,
                        ),
                )

            else ->
                this.also {
                    log.error { "Invalid message for this state received: $message" }
                }
        }
}

class LobbyGameState(
    val sendGpgnetMessage: (FromGameMessage) -> Unit,
    val lobby: GameLobby,
) : GameState {
    init {
        sendGpgnetMessage(GameStateMessage(GameStateEnum.LOBBY))
    }

    override fun process(message: ToGameMessage): GameState =
        when (message) {
            is HostGameMessage -> {
                log.info { "Hosting game on map ${message.mapName}" }
                this
            }

            is ConnectToPeerMessage -> {
                lobby.connectTo(
                    playerId = PlayerId(message.remotePlayerId),
                    localPort = 0,
                    destination = message.destination,
                )
                this
            }

            is JoinGameMessage -> {
                lobby.connectTo(
                    playerId = PlayerId(message.remotePlayerId),
                    localPort = 0,
                    destination = message.destination,
                )
                this
            }

            is DisconnectFromPeerMessage -> {
                lobby.disconnectFrom(PlayerId(message.remotePlayerId))
                this
            }

            else -> {
                log.error { "Invalid message for this state received: $message" }
                this
            }
        }
}
