package com.faforever.fa

import com.faforever.gpgnet.ConnectToPeerMessage
import com.faforever.gpgnet.DisconnectFromPeerMessage
import com.faforever.gpgnet.GameState
import com.faforever.gpgnet.GameStateMessage
import com.faforever.gpgnet.GpgnetMessage
import com.faforever.gpgnet.HostGameMessage
import com.faforever.gpgnet.JoinGameMessage
import com.faforever.gpgnet.ReceivedMessage
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

private val log = KotlinLogging.logger {}

class GpgnetProcess(
    gpgnetOption: LaunchOptions.Gpgnet,
    userOptions: UserOptions,
    commanderOptions: CommanderOptions,
    gameProcessOptions: GameProcessOptions,
) : AutoCloseable {
    private val gpgnetSocket =
        Socket(
            gpgnetOption.lobbyServer.host,
            gpgnetOption.lobbyServer.port,
        )

    private val writer = PrintWriter(gpgnetSocket.getOutputStream(), true)
    private val reader = BufferedReader(InputStreamReader(gpgnetSocket.getInputStream()))

    private val objectMapper = jacksonObjectMapper()

    private val gpgnetListenerThread: Thread =
        Thread({ gpgnetLoop() }, "gpgnetListener")
            .apply { start() }

    fun gpgnetLoop() {
        log.info { "gpgnetLoop started" }

        sendGameState(GameState.IDLE)

        reader.lines()
            .map { objectMapper.readValue<ReceivedMessage>(it).tryParse() }
            .forEach { message ->
                log.debug { "Received GpgNet message: $message" }
                check(message is GpgnetMessage.ToGameMessage) {
                    "Received invalid or unparseable message $message"
                }

                when (message) {
                    is HostGameMessage -> sendGameState(GameState.LOBBY)
                    is JoinGameMessage -> sendGameState(GameState.LOBBY)
                    is ConnectToPeerMessage -> TODO("Open UDP port")
                    is DisconnectFromPeerMessage -> TODO("Close UDP port")
                    else -> Unit
                }
            }
    }

    private fun sendGameState(gameState: GameState) {
        log.info { "Setting gameState to $gameState" }
        sendGpgnetMessage(GameStateMessage(GameState.IDLE))
    }

    private fun sendGpgnetMessage(fromGameMessage: GpgnetMessage.FromGameMessage) {
        val message = objectMapper.writeValueAsString(fromGameMessage)
        log.debug { "Sending GpgNet message: $message" }
        writer.write(message)
    }

    override fun close() {
        writer.close()
        reader.close()
        gpgnetSocket.close()
    }
}
