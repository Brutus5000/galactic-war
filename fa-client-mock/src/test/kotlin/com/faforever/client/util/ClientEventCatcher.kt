package com.faforever.client.util

import com.faforever.client.event.ClientEvent
import com.faforever.client.event.ClientEventListener
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

data class ClientEventCatcher(
    val eventType: Class<ClientEvent>,
) : ClientEventListener(filter = { it.javaClass == eventType }) {
    var caught: Boolean = false
        private set

    override fun onEvent(clientEvent: ClientEvent) {
        log.debug { "Fired for $clientEvent" }
        caught = true
    }
}
