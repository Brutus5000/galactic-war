package com.faforever.gpgnet

import com.faforever.gpgnet.GpgnetMessage.ToGameMessage

data class CreateLobbyMessage(
    val lobbyInitMode: LobbyInitMode,
    val lobbyPort: Int,
    val localPlayerName: String,
    val localPlayerId: Int,
    val unknownParameter: Int = 1,
) : ToGameMessage {
    companion object {
        const val COMMAND = "CreateLobby"
    }

    override val command = COMMAND
    override val args = listOf(
        lobbyInitMode.faId,
        lobbyPort,
        localPlayerName,
        localPlayerId,
        unknownParameter,
    )
}

data class HostGameMessage(val mapName: String) : ToGameMessage {
    companion object {
        const val COMMAND = "HostGame"
    }

    override val command = COMMAND
    override val args = listOf(mapName)
}

data class JoinGameMessage(val remotePlayerLogin: String, val remotePlayerId: Int, val destination: String) :
    ToGameMessage {
    companion object {
        const val COMMAND = "JoinGame"
    }

    override val command = COMMAND
    override val args = listOf(destination, remotePlayerLogin, remotePlayerId)
}

data class ConnectToPeerMessage(val remotePlayerLogin: String, val remotePlayerId: Int, val destination: String) :
    ToGameMessage {
    companion object {
        const val COMMAND = "ConnectToPeer"
    }

    override val command = COMMAND
    override val args = listOf(destination, remotePlayerLogin, remotePlayerId)
}

data class DisconnectFromPeerMessage(val remotePlayerId: Int) : ToGameMessage {
    companion object {
        const val COMMAND = "DisconnectFromPeer"
    }

    override val command = COMMAND
    override val args = listOf(remotePlayerId)
}
