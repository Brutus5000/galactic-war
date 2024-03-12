package com.faforever.fa

data class Endpoint(
    val host: String,
    val port: Int,
)

sealed interface LaunchOptions {
    data class Gpgnet(
        val lobbyServer: Endpoint,
        val replayServer: Endpoint?,
        val expectedPlayers: Int?,
    ) : LaunchOptions

    data class Replay(
        val replayId: Long,
        val replayPath: String,
    ) : LaunchOptions

    object Offline : LaunchOptions
}

data class GameProcessOptions(
    val initLuaPath: String,
    val noBugReport: Boolean = false,
    val logPath: String? = null,
)

data class UserOptions(
    val mean: Double? = null,
    val deviation: Double? = null,
    val division: String? = null,
    val subdivision: String? = null,
    val country: String? = null,
    val clanTag: String? = null,
    val numberOfRatedGames: Int? = null,
)

enum class Faction {
    AEON,
    CYBRAN,
    SERAPHIM,
    UEF,
}

data class CommanderOptions(
    val faction: Faction?,
    val teamId: Int?,
    val mapPosition: Int?,
)
