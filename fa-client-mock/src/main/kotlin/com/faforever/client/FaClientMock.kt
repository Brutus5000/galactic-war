package com.faforever.client

import com.faforever.client.event.ClientEvent
import com.faforever.client.event.ClientEventListener
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

class FaClientMock : AutoCloseable {
    private val gpgnetServer =
        GpgnetServer(
            publishEvent = ::publishEvent,
        )
    val gpgnetPort: Int? get() = gpgnetServer.port

    private val eventListeners = mutableListOf<ClientEventListener>()
    private var closed = false

    fun connectToGame() {
        log.info { "Connecting to game on gpgnet port $gpgnetPort" }
        gpgnetServer.runLoop()
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
