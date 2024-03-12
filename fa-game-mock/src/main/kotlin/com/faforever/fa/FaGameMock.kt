package com.faforever.fa

import com.faforever.fa.event.GameEvent
import com.faforever.fa.event.GameEventListener
import com.faforever.fa.event.OnEventHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

class FaGameMock(
    launchOptions: LaunchOptions,
    userOptions: UserOptions = UserOptions(),
    commanderOptions: CommanderOptions = CommanderOptions(),
    gameProcessOptions: GameProcessOptions,
) : AutoCloseable {
    private val gpgnetProcess: GpgnetClient

    private val eventListeners = mutableListOf<GameEventListener>()

    init {
        eventListeners.add(OnEventHandler(GameEvent.LobbyOpened::class.java, ::onLobbyOpened))
        when (launchOptions) {
            is LaunchOptions.Gpgnet ->
                gpgnetProcess =
                    GpgnetClient(
                        gpgnetOption = launchOptions,
                        userOptions = userOptions,
                        commanderOptions = commanderOptions,
                        gameProcessOptions = gameProcessOptions,
                        publishEvent = ::publishEvent,
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

    private fun onLobbyOpened(event: GameEvent.LobbyOpened) {
        val gameLobby = event.gameLobby
        log.debug { "lobby opened for playerId ${gameLobby.localPlayerId}" }

        Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(
                { gameLobby.broadcast(LocalDateTime.now().toString().toByteArray()) },
                1,
                1,
                TimeUnit.SECONDS,
            )
    }

    override fun close() {
        gpgnetProcess.close()
    }

    fun addEventListener(listener: GameEventListener) {
        eventListeners.add(listener)
    }

    private fun publishEvent(clientEvent: GameEvent) {
        eventListeners.forEach { it.onEventUnfiltered(clientEvent) }
    }
}
