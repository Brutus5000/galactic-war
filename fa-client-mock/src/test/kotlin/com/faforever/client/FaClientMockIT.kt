package com.faforever.client

import com.faforever.client.event.ClientEvent
import com.faforever.client.util.ClientEventCatcher
import com.faforever.client.util.GameEventCatcher
import com.faforever.fa.Endpoint
import com.faforever.fa.FaGameMock
import com.faforever.fa.GameProcessOptions
import com.faforever.fa.LaunchOptions
import com.faforever.fa.event.GameEvent
import io.kotest.assertions.nondeterministic.until
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class FaClientMockIT {
    @Test
    fun sendingAndReceivingData() {
        val client1ConnectedCatcher = ClientEventCatcher(ClientEvent.IdleGameState.javaClass)
        val client2ConnectedCatcher = ClientEventCatcher(ClientEvent.IdleGameState.javaClass)
        val client1LobbyStateCatcher = ClientEventCatcher(ClientEvent.LobbyGameState.javaClass)
        val client2LobbyStateCatcher = ClientEventCatcher(ClientEvent.LobbyGameState.javaClass)
        val game1LobbyOpened = GameEventCatcher(GameEvent.LobbyOpened::class.java)
        val game2LobbyOpened = GameEventCatcher(GameEvent.LobbyOpened::class.java)
        val game1DataSent = GameEventCatcher(GameEvent.DataSent.javaClass)
        val game2DataReceived = GameEventCatcher(GameEvent.DataReceived::class.java)

        val client1 =
            FaClientMock(lobbyPort = 5001, userName = "user1", userId = 1).apply {
                addEventListener(client1ConnectedCatcher)
                addEventListener(client1LobbyStateCatcher)
            }
        val game1 =
            localGame(client1.gpgnetPort).apply {
                addEventListener(game1LobbyOpened)
                addEventListener(game1DataSent)
                runInVirtualThread()
            }

        val client2 =
            FaClientMock(lobbyPort = 6001, userName = "user2", userId = 2).apply {
                addEventListener(client2ConnectedCatcher)
                addEventListener(client2LobbyStateCatcher)
            }
        val game2 =
            localGame(client2.gpgnetPort).apply {
                addEventListener(game2LobbyOpened)
                addEventListener(game2DataReceived)
                runInVirtualThread()
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

        game1LobbyOpened.caught shouldBe false
        game2LobbyOpened.caught shouldBe false

        client1.openLobby()
        client2.openLobby()

        runBlocking {
            until(10.seconds) {
                client1LobbyStateCatcher.caught shouldBe true
                client2LobbyStateCatcher.caught shouldBe true
                game1LobbyOpened.caught shouldBe true
                game2LobbyOpened.caught shouldBe true
            }
        }

        game1DataSent.caught shouldBe false
        game2DataReceived.caught shouldBe false

        client1.hostLobby()
        client2.joinRemoteLobby(playerLogin = "user2", playerId = 2, host = "127.0.0.1", port = 5001)
        client1.connectToPeer(playerLogin = "user1", playerId = 1, host = "127.0.0.1", port = 6001)

        runBlocking {
            until(1.seconds) {
                game1DataSent.caught shouldBe true
                game2DataReceived.caught shouldBe true
            }
        }
    }

    private fun localGame(port: Int) =
        FaGameMock(
            launchOptions = LaunchOptions.Gpgnet(lobbyServer = Endpoint("127.0.0.1", port)),
            gameProcessOptions = GameProcessOptions(initLuaPath = "./init.lua"),
        )
}
