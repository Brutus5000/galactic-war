package com.faforever.fa

import com.faforever.client.FaClientMock
import com.faforever.client.event.ClientEvent
import com.faforever.client.event.ClientEventListener
import io.kotest.assertions.nondeterministic.until
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class FaGameMockIT {
    data class EventCatcher(
        val eventType: Class<ClientEvent>,
    ) : ClientEventListener(filter = { it.javaClass == eventType }) {
        var caught: Boolean = false
            private set

        override fun onEvent(clientEvent: ClientEvent) {
            caught = true
        }
    }

    @Test
    fun connect() {
        val isClient1Connected = EventCatcher(ClientEvent.ConnectedToGame.javaClass)

        val gameClient1 =
            FaClientMock().apply {
                addEventListener(isClient1Connected)
            }

        val game1 =
            FaGameMock(
                launchOptions = LaunchOptions.Gpgnet(lobbyServer = Endpoint("localhost", gameClient1.gpgnetPort!!)),
                gameProcessOptions = GameProcessOptions(initLuaPath = "/init.lua"),
            ).also {
                it.runInVirtualThread()
            }

        Thread.startVirtualThread {
            gameClient1.connectToGame()
        }

        runBlocking {
            until(1.seconds) {
                isClient1Connected.caught shouldBe true
            }
        }
    }
}
