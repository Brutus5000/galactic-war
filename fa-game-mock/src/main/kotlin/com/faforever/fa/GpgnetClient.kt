package com.faforever.fa

import com.faforever.fa.event.GameEvent
import com.faforever.fa.util.SocketFactory
import com.faforever.gpgnet.io.FaStreamReader
import com.faforever.gpgnet.io.FaStreamWriter
import com.faforever.gpgnet.protocol.GpgnetMessage
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging

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

    private val writer = FaStreamWriter(gpgnetSocket.getOutputStream())
    private val reader = FaStreamReader(gpgnetSocket.getInputStream())

    private val objectMapper = jacksonObjectMapper()

    fun runLoop() {
        log.info { "gpgnetLoop started" }

        Thread.startVirtualThread {
            while (!Thread.interrupted()) {
                val message = reader.readMessage()
                log.debug { "Received GpgNet message from client: $message" }
                check(message is GpgnetMessage.ToGameMessage) {
                    "Received invalid or unparseable message $message"
                }

                gameState = gameState.process(message)
            }
        }

        gameState = IdleGameState(sendGpgnetMessage = this::sendGpgnetMessage, publishEvent = publishEvent)
    }

    private fun sendGpgnetMessage(fromGameMessage: GpgnetMessage.FromGameMessage) {
        log.debug { "Sending GpgNet message: $fromGameMessage" }
        writer.writeMessage(fromGameMessage)
    }

    override fun close() {
        writer.close()
        reader.close()
        gpgnetSocket.close()
    }
}
