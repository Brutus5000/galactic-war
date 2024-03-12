package com.faforever.fa

import com.faforever.fa.GameLobby.PlayerId
import com.faforever.fa.event.GameEvent
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
    val publishEvent: (GameEvent) -> Unit,
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
                            publishEvent = publishEvent,
                        ),
                    publishEvent = publishEvent,
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
    val publishEvent: (GameEvent) -> Unit,
) : GameState {
    init {
        sendGpgnetMessage(GameStateMessage(GameStateEnum.LOBBY))
    }

    override fun process(message: ToGameMessage): GameState =
        when (message) {
            is HostGameMessage -> {
                when (lobby.connectionRole) {
                    ConnectionRole.UNDEFINED -> {
                        log.info { "Hosting game on map ${message.mapName}" }
                        lobby.connectionRole = ConnectionRole.HOST
                    }

                    else -> log.error { "Connection role already defined" }
                }
                this
            }

            is ConnectToPeerMessage -> {
                when (lobby.connectionRole) {
                    ConnectionRole.UNDEFINED -> log.error { "Connection role undefined" }
                    else ->
                        lobby.connectTo(
                            playerId = PlayerId(message.remotePlayerId),
                            destination = message.destination,
                        )
                }
                this
            }

            is JoinGameMessage -> {
                when (lobby.connectionRole) {
                    ConnectionRole.UNDEFINED -> {
                        lobby.connectionRole = ConnectionRole.CLIENT
                        lobby.connectTo(
                            playerId = PlayerId(message.remotePlayerId),
                            destination = message.destination,
                        )
                    }

                    else -> log.error { "Connection role already defined" }
                }
                this
            }

            is DisconnectFromPeerMessage -> {
                when (lobby.connectionRole) {
                    ConnectionRole.UNDEFINED -> log.error { "Connection role undefined" }
                    else -> lobby.disconnectFrom(PlayerId(message.remotePlayerId))
                }
                this
            }

            else -> {
                log.error { "Invalid message for this state received: $message" }
                this
            }
        }
}
