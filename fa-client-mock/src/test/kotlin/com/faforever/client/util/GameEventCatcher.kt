package com.faforever.client.util

import com.faforever.fa.event.GameEvent
import com.faforever.fa.event.GameEventListener
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

data class GameEventCatcher<T : GameEvent>(
    val eventType: Class<T>,
) : GameEventListener(filter = { it.javaClass == eventType }) {
    var caught: Boolean = false
        private set

    override fun onEvent(gameEvent: GameEvent) {
        log.debug { "Fired for $gameEvent" }
        caught = true
    }
}
