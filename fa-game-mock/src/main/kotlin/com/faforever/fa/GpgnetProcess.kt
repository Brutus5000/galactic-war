package com.faforever.fa

import com.faforever.fa.util.SocketFactory
import com.faforever.gpgnet.protocol.GpgnetMessage
import com.faforever.gpgnet.protocol.ReceivedMessage
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter

private val log = KotlinLogging.logger {}

class GpgnetProcess(
    gpgnetOption: LaunchOptions.Gpgnet,
    userOptions: UserOptions,
    commanderOptions: CommanderOptions,
    gameProcessOptions: GameProcessOptions,
) : AutoCloseable {
    private val gpgnetSocket =
        SocketFactory.createLocalTCPClientSocket(
            gpgnetOption.lobbyServer.host,
            gpgnetOption.lobbyServer.port,
        )

    private lateinit var gameState: GameState

    private val writer = PrintWriter(gpgnetSocket.getOutputStream(), true)
    private val reader = BufferedReader(InputStreamReader(gpgnetSocket.getInputStream()))

    private val objectMapper = jacksonObjectMapper()

    fun runLoop() {
        log.info { "gpgnetLoop started" }

        gameState = IdleGameState(sendGpgnetMessage = this::sendGpgnetMessage)

        reader.lines()
            .map { objectMapper.readValue<ReceivedMessage>(it).tryParse() }
            .forEach { message ->
                log.debug { "Received GpgNet message: $message" }
                check(message is GpgnetMessage.ToGameMessage) {
                    "Received invalid or unparseable message $message"
                }

                gameState = gameState.process(message)
            }
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
