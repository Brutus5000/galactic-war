package com.faforever.client

import com.faforever.client.event.ClientEvent
import com.faforever.client.util.EventCatcher
import com.faforever.fa.Endpoint
import com.faforever.fa.FaGameMock
import com.faforever.fa.GameProcessOptions
import com.faforever.fa.LaunchOptions
import io.kotest.assertions.nondeterministic.until
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class FaClientMockIT {
    @Test
    fun test() {
        val client1ConnectedCatcher = EventCatcher(ClientEvent.IdleGameState.javaClass)
        val client2ConnectedCatcher = EventCatcher(ClientEvent.IdleGameState.javaClass)
        val client1LobbyStateCatcher = EventCatcher(ClientEvent.LobbyGameState.javaClass)
        val client2LobbyStateCatcher = EventCatcher(ClientEvent.LobbyGameState.javaClass)

        val client1 =
            FaClientMock(lobbyPort = 5001, userName = "user1", userId = 1).apply {
                addEventListener(client1ConnectedCatcher)
                addEventListener(client1LobbyStateCatcher)
            }
        val game1 =
            localGame(client1.gpgnetPort).also {
                it.runInVirtualThread()
            }

        val client2 =
            FaClientMock(lobbyPort = 6001, userName = "user2", userId = 2).apply {
                addEventListener(client2ConnectedCatcher)
                addEventListener(client2LobbyStateCatcher)
            }
        val game2 =
            localGame(client2.gpgnetPort).also {
                it.runInVirtualThread()
            }

        Thread.startVirtualThread {
            client1.connectToGame()
        }

        Thread.startVirtualThread {
            client2.connectToGame()
        }

        runBlocking {
            until(1.seconds) {
                client1ConnectedCatcher.caught shouldBe true
                client2ConnectedCatcher.caught shouldBe true
            }
        }

        client1.openLobby()
        client2.openLobby()

        runBlocking {
            until(10.seconds) {
                client1LobbyStateCatcher.caught shouldBe true
                client2LobbyStateCatcher.caught shouldBe true
            }
        }

        client1.hostLobby()
        client2.joinRemoteLobby(playerLogin = "user2", playerId = 2, host = "localhost", port = 6001)
        client1.connectToPeer(playerLogin = "user2", playerId = 2, host = "localhost", port = 6001)
    }

    private fun localGame(port: Int) =
        FaGameMock(
            launchOptions = LaunchOptions.Gpgnet(lobbyServer = Endpoint("localhost", port)),
            gameProcessOptions = GameProcessOptions(initLuaPath = "./init.lua"),
        )
}
