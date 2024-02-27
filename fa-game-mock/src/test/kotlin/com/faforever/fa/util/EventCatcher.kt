package com.faforever.fa.util

import com.faforever.client.event.ClientEvent
import com.faforever.client.event.ClientEventListener

data class EventCatcher(
    val eventType: Class<ClientEvent>,
) : ClientEventListener(filter = { it.javaClass == eventType }) {
    var caught: Boolean = false
        private set

    override fun onEvent(clientEvent: ClientEvent) {
        caught = true
    }
}
