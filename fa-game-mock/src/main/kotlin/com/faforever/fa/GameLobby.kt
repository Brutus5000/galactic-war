package com.faforever.fa

import com.faforever.fa.util.SocketFactory
import com.faforever.gpgnet.protocol.LobbyInitMode
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.Closeable
import java.net.DatagramPacket
import java.net.InetSocketAddress

private val log = KotlinLogging.logger {}

enum class ConnectionRole {
    UNDEFINED,
    HOST,
    CLIENT,
}

data class GameLobby(
    val lobbyInitMode: LobbyInitMode,
    val lobbyPort: Int,
    val localPlayerName: String,
    val localPlayerId: Int,
    val bufferSize: Int = 65536,
    var connectionRole: ConnectionRole = ConnectionRole.UNDEFINED,
) : Closeable {
    @JvmInline
    value class PlayerId(val id: Int)

    private data class PlayerConnection(
        val playerId: PlayerId,
        val destination: InetSocketAddress,
    )

    @Volatile
    var closing: Boolean = false
        private set

    private val playerConnections: MutableMap<PlayerId, PlayerConnection> = mutableMapOf()
    private val lobbySocket = SocketFactory.createLocalUDPSocket(lobbyPort)
    private val readingThread: Thread =
        Thread(this::listeningLoop).apply {
            setUncaughtExceptionHandler { thread, throwable ->
                log.error(throwable) { "Unexpected error in reading thread ${thread.name}: closing now)" }
                close()
            }
            start()
        }
    private val buffer = ByteArray(bufferSize)

    fun connectTo(
        playerId: PlayerId,
        localPort: Int,
        destination: InetSocketAddress,
    ) {
        playerConnections[playerId] =
            PlayerConnection(
                playerId = playerId,
                destination = destination,
            )
    }

    fun disconnectFrom(playerId: PlayerId) {
        playerConnections.remove(playerId)
    }

    fun sendToPlayer(
        playerId: PlayerId,
        data: ByteArray,
    ) {
        val connection = checkNotNull(playerConnections[playerId]) { "Unknown playerId: $playerId" }
        val packet = DatagramPacket(data, 0, data.size, connection.destination)
        lobbySocket.send(packet)
    }

    private fun listeningLoop() {
        log.info { "Listening for messages" }

        while (true) {
            if (closing) return

            val packet = DatagramPacket(buffer, buffer.size)

            try {
                lobbySocket.receive(packet)
                log.trace { "Received ${packet.length} bytes" }
                onDataReceived(buffer.copyOfRange(0, packet.length))
            } catch (e: Exception) {
                if (closing) {
                    return
                } else {
                    throw e
                }
            }
        }
    }

    private fun onDataReceived(bytes: ByteArray) {
        log.info { "Received: ${bytes.toString(Charsets.UTF_8)}" }
    }

    override fun close() {
        closing = true
        lobbySocket.close()
    }
}
