package com.faforever.gpgnet.protocol

import com.fasterxml.jackson.annotation.JsonIncludeProperties
import java.net.InetSocketAddress

@JsonIncludeProperties(value = ["command", "args"])
interface GpgnetMessage {
    interface ToGameMessage : GpgnetMessage

    interface FromGameMessage : GpgnetMessage

    val command: String
    val args: List<Any>
}

data class ReceivedMessage(override val command: String, override val args: List<Any>) : GpgnetMessage {
    fun tryParse() =
        when (command) {
            // Lobby messages
            CreateLobbyMessage.COMMAND ->
                CreateLobbyMessage(
                    LobbyInitMode.parseFromFaId(args[0] as Int),
                    args[1] as Int,
                    args[2] as String,
                    args[3] as Int,
                    args[4] as Int,
                )

            HostGameMessage.COMMAND -> HostGameMessage(args[0] as String)
            JoinGameMessage.COMMAND -> JoinGameMessage(args[1] as String, args[2] as Int, parseAddress(args[0]))
            ConnectToPeerMessage.COMMAND -> ConnectToPeerMessage(args[1] as String, args[2] as Int, parseAddress(args[0]))
            DisconnectFromPeerMessage.COMMAND -> DisconnectFromPeerMessage(args[0] as Int)
            // Game messages
            GameStateMessage.COMMAND -> GameStateMessage(GameStateEnum.parseGpgnetString(args[0] as String))
            GameEndedMessage.COMMAND -> GameEndedMessage()
            else -> this
        }

    private fun parseAddress(destination: Any): InetSocketAddress {
        val split = (destination as String).split(":")
        return InetSocketAddress.createUnresolved(split[0], split[1].toInt())
    }
}
