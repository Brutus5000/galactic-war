package com.faforever.fa

import com.faforever.fa.event.GameEvent
import com.faforever.fa.util.SocketFactory
import com.faforever.gpgnet.protocol.GpgnetMessage
import com.faforever.gpgnet.protocol.ReceivedMessage
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

private val log = KotlinLogging.logger {}

class GpgnetClient(
    gpgnetOption: LaunchOptions.Gpgnet,
    userOptions: UserOptions,
    commanderOptions: CommanderOptions,
    gameProcessOptions: GameProcessOptions,
    val publishEvent: (GameEvent) -> Unit,
) : AutoCloseable {
    private val gpgnetSocket =
        SocketFactory.createLocalTCPClientSocket(
            gpgnetOption.lobbyServer.host,
            gpgnetOption.lobbyServer.port,
        ).also {
            log.info { "gpgnet socket connected (localPort=${it.localPort}, port=${it.port})" }
        }

    private lateinit var gameState: GameState

    private val writer = BufferedWriter(OutputStreamWriter(gpgnetSocket.getOutputStream()))
    private val reader = BufferedReader(InputStreamReader(gpgnetSocket.getInputStream()))

    private val objectMapper = jacksonObjectMapper()

    fun runLoop() {
        log.info { "gpgnetLoop started" }

        Thread {
            reader.lines()
                .filter { it.isNotBlank() }
                .map { objectMapper.readValue<ReceivedMessage>(it).tryParse() }
                .forEach { message ->
                    log.debug { "Received GpgNet message from client: $message" }
                    check(message is GpgnetMessage.ToGameMessage) {
                        "Received invalid or unparseable message $message"
                    }

                    gameState = gameState.process(message)
                }
        }.start()

        gameState = IdleGameState(sendGpgnetMessage = this::sendGpgnetMessage, publishEvent = publishEvent)
    }

    private fun sendGpgnetMessage(fromGameMessage: GpgnetMessage.FromGameMessage) {
        val message = objectMapper.writeValueAsString(fromGameMessage)
        log.debug { "Sending GpgNet message: $message" }
        writer.write(message)
        writer.newLine()
        writer.flush()
    }

    override fun close() {
        writer.close()
        reader.close()
        gpgnetSocket.close()
    }
}
