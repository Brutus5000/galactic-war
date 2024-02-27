package com.faforever.client

import com.faforever.client.event.ClientEvent
import com.faforever.client.event.ClientEventListener
import com.faforever.gpgnet.protocol.CreateLobbyMessage
import com.faforever.gpgnet.protocol.LobbyInitMode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

class FaClientMock(
    val lobbyPort: Int = 5001,
    val userName: String = "FaClientMock",
    val userId: Int = 666,
) : AutoCloseable {
    private val gpgnetServer = GpgnetServer(publishEvent = ::publishEvent)
    private val objectMapper = jacksonObjectMapper()

    val gpgnetPort: Int get() = gpgnetServer.port

    private val eventListeners = mutableListOf<ClientEventListener>()
    private var closed = false

    fun connectToGame() {
        check(gpgnetServer.gameState is InitGameState)

        log.info { "Connecting to game on gpgnet port $gpgnetPort" }
        gpgnetServer.runLoop()
    }

    fun openLobby() {
        check(gpgnetServer.gameState is IdleGameState)

        gpgnetServer.sendGpgnetMessage(
            CreateLobbyMessage(
                lobbyInitMode = LobbyInitMode.AUTO,
                lobbyPort = lobbyPort,
                localPlayerName = userName,
                localPlayerId = userId,
            ),
        )
    }

    override fun close() {
        gpgnetServer.close()
    }

    fun addEventListener(listener: ClientEventListener) {
        eventListeners.add(listener)
    }

    private fun publishEvent(clientEvent: ClientEvent) {
        eventListeners.forEach { it.onEventUnfiltered(clientEvent) }
    }
}
