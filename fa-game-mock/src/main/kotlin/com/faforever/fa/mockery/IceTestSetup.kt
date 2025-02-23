package com.faforever.fa.mockery

import com.faforever.fa.*
import com.faforever.fa.util.SocketFactory
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.math.absoluteValue
import kotlin.random.Random

private fun rpcCallSetIceServers(coturnHost: String, username: String, credential: String) = """
    {
      "method": "setIceServers",
      "id": 1,
      "params": [
        [
          {
            "urls": [
              "turn://$coturnHost:3478?transport=tcp",
              "stun://$coturnHost:3478",
              "turn://$coturnHost:3478?transport=udp"
            ],
            "username": "$username",
            "credential": "$credential"
          }
        ]
      ],
      "jsonrpc": "2.0"
    }
""".trimIndent()

private fun rpcCallJoinGame() = """
    {
      "method": "joinGame",
      "id": 2,
      "params": [
          "theOtherSide",
          666
      ],
      "jsonrpc": "2.0"
    }
""".trimIndent()

private fun rpcCallHostGame() = """{"method":"hostGame","id":2,"params":["scmp_013"],"jsonrpc":"2.0"}"""

// {"method":"connectToPeer","id":3,"params":["p4block",184746,true],"jsonrpc":"2.0"}

private fun rpcCallConnectToPeer() = """
    {
      "method": "connectToPeer",
      "id": 3,
      "params": [
          "theOtherSide",
          666,
          true
      ],
      "jsonrpc": "2.0"
    }
""".trimIndent()

fun main(args: Array<String>) {
    val userId = Random.nextInt().absoluteValue
    val turnHostName = "p4turn.faforever.xyz"
    val turnUsername = ""
    val turnCredentials = ""
    val gameId = 123
    val rpcPort = 30000
    val gpgPort = 31000

    // Start ICE Adapter
    val workDirectory = Path.of(System.getProperty("nativeDir", "lib")).toAbsolutePath()
    val cmd: List<String> = buildCommand(userId, workDirectory, rpcPort, gpgPort, gameId)

    val iceAdapterProcess = ProcessBuilder().apply {
        directory(workDirectory.toFile())
        command(cmd)
    }.start()

    iceAdapterProcess.onExit().thenAccept { finished: Process ->
        val exitCode = finished.exitValue()
        if (exitCode == 0) {
            println("ICE adapter terminated normally")
        } else {
            println("ICE adapter terminated with exit code: $exitCode")
        }
    }

    Thread.startVirtualThread {
        iceAdapterProcess.inputStream.use { stream ->
            stream.bufferedReader().use { reader ->
                reader.lines().forEach { println("~~ICE:STDOUT~~ $it") }
            }
        }
    }
    Thread.startVirtualThread {
        iceAdapterProcess.errorStream.use { stream ->
            stream.bufferedReader().use { reader ->
                reader.lines().forEach { println("~~ICE:STDERR~~ $it") }
            }
        }
    }

    val rpcClientSocket = SocketFactory.createLocalTCPClientSocket("localhost", rpcPort)
    println("ICE adapter connected to RPC")

    // read rpc message
    Thread.startVirtualThread {
        rpcClientSocket.getInputStream().use { stream ->
            stream.bufferedReader().use { reader ->
                reader.lines()
                    .forEach { println("~~RPC~~ $it") }
            }
        }
    }

    val gpgClientSocket = SocketFactory.createLocalTCPClientSocket("localhost", gpgPort)
    println("Connected to GPG socket as client")

    rpcClientSocket.getOutputStream().write(rpcCallSetIceServers(turnHostName, turnUsername, turnCredentials ).toByteArray())
    println("Set ice servers on ice adapter")

    // Start game mock
    val gameMock = FaGameMock(
        launchOptions = LaunchOptions.Gpgnet(
            lobbyServer = Endpoint("127.0.0.1", gpgPort),
        ),
        userOptions = UserOptions(),
        commanderOptions = CommanderOptions(),
        gameProcessOptions = GameProcessOptions(initLuaPath = "init.lua"),
    )

    gameMock.runInVirtualThread()

    Thread.sleep(5000)

    println("Host game")
    rpcClientSocket.getOutputStream().write(rpcCallHostGame().toByteArray())

    Thread.sleep(1000)
    rpcClientSocket.getOutputStream().write(rpcCallConnectToPeer().toByteArray())

     while (true) {
        Thread.sleep(1000)
     }
}

private fun getJavaFXClassPathJars(): String {
    return JavaUtil.CLASS_PATH_LIST.stream()
        .filter { s -> s.contains("javafx-") }
        .collect(Collectors.joining(JavaUtil.CLASSPATH_SEPARATOR))
}

private fun getBinaryName(workDirectory: Path): String {
    return workDirectory.resolve("faf-ice-adapter-3.3.11-linux.jar").toString()
}

fun buildCommand(
    userId: Int,
    workDirectory: Path,
    rpcPort: Int,
    gpgPort: Int,
    gameId: Int
): List<String> {
    val classpath: String = getBinaryName(workDirectory) + JavaUtil.CLASSPATH_SEPARATOR + getJavaFXClassPathJars()

    val cmd: MutableList<String> = ArrayList()
    cmd.add(
        Path.of(System.getProperty("java.home"))
            .resolve("bin")
            .resolve("java")
            .toAbsolutePath()
            .toString()
    )

    // if (!forgedAlliancePrefs.isAllowIpv6()) {
    //     cmd.add("-Dorg.ice4j.ipv6.DISABLED=true")
    // }

    val standardIceOptions = listOf<String>(
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005",
        "-cp", classpath,
        "com.faforever.iceadapter.IceAdapter",
        "--id", userId.toString(),
        "--game-id", gameId.toString(),
        "--login", "user_$userId",
        "--rpc-port", rpcPort.toString(), "--gpgnet-port", gpgPort.toString(), "--access-token",
        // tokenRetriever.getRefreshedTokenValue().block(), "--icebreaker-base-url",
        // clientProperties.getApi().getBaseUrl() + "/ice"
    )

    cmd.addAll(standardIceOptions)

    cmd.add("--debug-window")
    cmd.add("--info-window")

    return cmd
}
