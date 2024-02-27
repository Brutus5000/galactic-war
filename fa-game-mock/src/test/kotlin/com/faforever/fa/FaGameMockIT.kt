package com.faforever.fa

import com.faforever.client.FaClientMock
import com.faforever.client.event.ClientEvent
import com.faforever.fa.util.EventCatcher
import io.kotest.assertions.nondeterministic.until
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class FaGameMockIT {
    @Test
    fun connect() {
        // prepare
        val clientConnectedCatcher = EventCatcher(ClientEvent.IdleGameState.javaClass)

        val gameClient =
            FaClientMock().apply {
                addEventListener(clientConnectedCatcher)
            }

        val game =
            FaGameMock(
                launchOptions = LaunchOptions.Gpgnet(lobbyServer = Endpoint("localhost", gameClient.gpgnetPort)),
                gameProcessOptions = GameProcessOptions(initLuaPath = "/init.lua"),
            ).also {
                it.runInVirtualThread()
            }

        clientConnectedCatcher.caught shouldBe false

        // execute
        Thread.startVirtualThread {
            gameClient.connectToGame()
        }

        // verify
        runBlocking {
            until(1.seconds) {
                clientConnectedCatcher.caught shouldBe true
            }
        }

        gameClient.close()
        game.close()
    }

    @Test
    fun createLobby() {
        // prepare
        val idleGameStateCatcher = EventCatcher(ClientEvent.IdleGameState.javaClass)
        val lobbyGameStateCatcher = EventCatcher(ClientEvent.LobbyGameState.javaClass)

        val gameClient =
            FaClientMock().apply {
                addEventListener(idleGameStateCatcher)
                addEventListener(lobbyGameStateCatcher)
            }

        val game =
            FaGameMock(
                launchOptions = LaunchOptions.Gpgnet(lobbyServer = Endpoint("localhost", gameClient.gpgnetPort)),
                gameProcessOptions = GameProcessOptions(initLuaPath = "/init.lua"),
            ).also {
                it.runInVirtualThread()
            }

        idleGameStateCatcher.caught shouldBe false

        Thread.startVirtualThread {
            gameClient.connectToGame()
        }

        runBlocking {
            until(1.seconds) {
                idleGameStateCatcher.caught shouldBe true
            }
        }

        // execute
        gameClient.openLobby()

        // verify
        runBlocking {
            until(1.seconds) {
                lobbyGameStateCatcher.caught shouldBe true
            }
        }

        gameClient.close()
        game.close()
    }
}
