package com.faforever.client

import com.faforever.client.event.ClientEvent
import com.faforever.client.util.SocketFactory
import com.faforever.gpgnet.io.FaStreamReader
import com.faforever.gpgnet.io.FaStreamWriter
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

class GpgnetServer(
    private val publishEvent: (ClientEvent) -> Unit,
) : AutoCloseable {
    private val gpgnetSocket =
        SocketFactory.createLocalTCPServerSocket().also {
            log.info { "Opened gpgnet server socket on port ${it.localPort}" }
        }

    var gameState: GameState =
        InitGameState(
            publishEvent = publishEvent,
            sendGpgnetMessage = ::sendGpgnetMessage,
        )
        private set

    private var writer: FaStreamWriter? = null
    private var reader: FaStreamReader? = null

    val port: Int get() = gpgnetSocket.localPort

    fun runLoop() {
        log.info { "GpgnetServer started" }

        runCatching {
            gpgnetSocket.accept().also {
                log.info { "Game connection accepted (localPort=${it.localPort}, port=${it.port})" }
                writer = FaStreamWriter(it.getOutputStream())
                reader = FaStreamReader(it.getInputStream())
            }

            while (!Thread.interrupted()) {
                val message = reader!!.readMessage()

                check(message is GpgnetMessage.FromGameMessage) {
                    "Received invalid or unparseable message $message"
                }

                gameState = gameState.receive(message)
            }
        }.onFailure {
            log.error(it) { "Game connection failed to process" }
        }

        writer?.close()
        reader?.close()
        log.info { "GpgnetServer closed" }
    }

    fun sendGpgnetMessage(toGameMessage: GpgnetMessage.ToGameMessage) {
        log.debug { "Sending GpgNet message: $toGameMessage" }
        writer!!.writeMessage(toGameMessage)
    }

    override fun close() {
        writer?.close()
        reader?.close()
        gpgnetSocket.close()
    }
}
