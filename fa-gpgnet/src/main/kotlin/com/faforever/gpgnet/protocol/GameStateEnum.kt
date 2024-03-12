package com.faforever.gpgnet.protocol

import com.faforever.gpgnet.protocol.GpgnetMessage.FromGameMessage

enum class GameStateEnum(val gpgnetString: String) {
    NONE("None"),
    IDLE("Idle"),
    LOBBY("Lobby"),
    LAUNCHING("Launching"),

    // Not in the original game, added by FAF project
    ENDED("Ended"),
    ;

    companion object {
        fun parseGpgnetString(string: String) = entries.first { string == it.gpgnetString }
    }
}

enum class LobbyInitMode(val faId: Int) {
    NORMAL(0),
    AUTO(1),
    ;

    companion object {
        fun parseFromFaId(faId: Int) = LobbyInitMode.entries.first { faId == it.faId }
    }
}

data class GameStateMessage(val gameState: GameStateEnum) : FromGameMessage {
    companion object {
        const val COMMAND = "GameState"
    }

    override val command = COMMAND
    override val args = listOf(gameState.gpgnetString)
}

data class GameEndedMessage(
    override val command: String = COMMAND,
) : FromGameMessage {
    companion object {
        const val COMMAND = "GameEnded"
    }

    override val args = listOf<Any>()
}
