package com.faforever.client

import com.faforever.client.event.ClientEvent
import com.faforever.client.event.ClientEventListener
import com.faforever.gpgnet.protocol.ConnectToPeerMessage
import com.faforever.gpgnet.protocol.CreateLobbyMessage
import com.faforever.gpgnet.protocol.HostGameMessage
import com.faforever.gpgnet.protocol.JoinGameMessage
import com.faforever.gpgnet.protocol.LobbyInitMode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.InetSocketAddress

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
        verifyGameState<InitGameState>()

        log.info { "Connecting to game on gpgnet port $gpgnetPort" }
        gpgnetServer.runLoop()
    }

    fun openLobby() {
        verifyGameState<IdleGameState>()

        gpgnetServer.sendGpgnetMessage(
            CreateLobbyMessage(
                lobbyInitMode = LobbyInitMode.AUTO,
                lobbyPort = lobbyPort,
                localPlayerName = userName,
                localPlayerId = userId,
            ),
        )
    }

    fun hostLobby(mapName: String = "someMap") {
        verifyGameState<LobbyGameState>()

        gpgnetServer.sendGpgnetMessage(
            HostGameMessage(mapName = mapName),
        )
    }

    fun joinRemoteLobby(
        playerLogin: String,
        playerId: Int,
        host: String,
        port: Int,
    ) {
        verifyGameState<LobbyGameState>()

        gpgnetServer.sendGpgnetMessage(
            JoinGameMessage(
                remotePlayerLogin = playerLogin,
                remotePlayerId = playerId,
                destination = InetSocketAddress(host, port),
            ),
        )
    }

    fun connectToPeer(
        playerLogin: String,
        playerId: Int,
        host: String,
        port: Int,
    ) {
        verifyGameState<LobbyGameState>()

        gpgnetServer.sendGpgnetMessage(
            ConnectToPeerMessage(
                remotePlayerLogin = playerLogin,
                remotePlayerId = playerId,
                destination = InetSocketAddress(host, port),
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

    private inline fun <reified T> verifyGameState(): T {
        check(gpgnetServer.gameState is T) { "gameState must be ${T::class.java}, but was ${gpgnetServer.gameState.javaClass}" }
        return gpgnetServer.gameState as T
    }
}
