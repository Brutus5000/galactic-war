package com.faforever.fa

import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

class FaGameMock(
    launchOptions: LaunchOptions,
    userOptions: UserOptions = UserOptions(),
    commanderOptions: CommanderOptions = CommanderOptions(),
    gameProcessOptions: GameProcessOptions,
) {
    private val gpgnetProcess: GpgnetClient

    init {
        when (launchOptions) {
            is LaunchOptions.Gpgnet ->
                gpgnetProcess =
                    GpgnetClient(
                        launchOptions,
                        userOptions,
                        commanderOptions,
                        gameProcessOptions,
                    )
            is LaunchOptions.Offline -> throw NotImplementedError("Offline launching is not supported.")
            is LaunchOptions.Replay -> throw NotImplementedError("Replay launching is not supported.")
        }
    }

    fun runInVirtualThread() {
        Thread.startVirtualThread {
            gpgnetProcess.runLoop()
        }
    }
}
