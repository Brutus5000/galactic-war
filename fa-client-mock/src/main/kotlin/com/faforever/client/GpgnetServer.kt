package com.faforever.client

import com.faforever.client.event.ClientEvent
import com.faforever.client.util.SocketFactory
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

    private lateinit var gameState: GameState

    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null

    private val objectMapper = jacksonObjectMapper()

    val port: Int? get() = gpgnetSocket.localPort

    fun runLoop() {
        log.info { "GpgnetServer started" }

        gameState =
            InitGameState(
                publishEvent = publishEvent,
                sendGpgnetMessage = ::sendGpgnetMessage,
            )

        runCatching {
            gpgnetSocket.accept().also {
                log.info { "Game connection accepted (localPort=${it.localPort}, port=${it.port})" }
                writer = BufferedWriter(OutputStreamWriter(it.getOutputStream()))
                reader = BufferedReader(InputStreamReader(it.getInputStream()))
            }

            reader!!.lines()
                .filter { it.isNotBlank() }
                .map { objectMapper.readValue<ReceivedMessage>(it).tryParse() }
                .forEach { message ->
                    log.debug { "Received GpgNet message from game: $message" }
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

    private fun sendGpgnetMessage(fromGameMessage: GpgnetMessage.FromGameMessage) {
        val message = objectMapper.writeValueAsString(fromGameMessage)
        log.debug { "Sending GpgNet message: $message" }
        writer!!.write(message)
        writer!!.newLine()
        writer!!.flush()
    }

    override fun close() {
        writer?.close()
        reader?.close()
        gpgnetSocket.close()
    }
}
