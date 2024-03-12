package com.faforever.fa.util

import com.faforever.fa.event.GameEvent
import com.faforever.fa.event.GameEventListener

data class GameEventCatcher(
    val eventType: Class<GameEvent>,
) : GameEventListener(filter = { it.javaClass == eventType }) {
    var caught: Boolean = false
        private set

    override fun onEvent(gameEvent: GameEvent) {
        caught = true
    }
}
