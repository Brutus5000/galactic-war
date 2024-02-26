package com.faforever.client.util

import dev.failsafe.Failsafe
import dev.failsafe.RetryPolicy
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.time.Duration
import kotlin.random.Random

private val log = KotlinLogging.logger {}

/**
 * Util class, which helps to build sockets and also allows mocking them for testing
 */
object SocketFactory {
    fun createLocalUDPSocket(
        portFrom: Int = 40_000,
        portTo: Int = 60_000,
        maxAttempts: Int = 10,
    ): DatagramSocket =
        // since we choose a random port, we'll try again in case the port is already in use
        Failsafe.with(
            RetryPolicy.builder<DatagramSocket>()
                .handle(SocketException::class.java)
                .withMaxAttempts(maxAttempts)
                .build(),
        ).get { _ ->
            createLocalUDPSocket(Random.nextInt(portFrom, portTo))
        }

    fun createLocalUDPSocket(port: Int): DatagramSocket = DatagramSocket(port, InetAddress.getLoopbackAddress())

    fun createLocalTCPServerSocket(
        portFrom: Int = 40_000,
        portTo: Int = 60_000,
        maxAttempts: Int = 10,
    ): ServerSocket =
        // since we choose a random port, we'll try again in case the port is already in use
        Failsafe.with(
            RetryPolicy.builder<ServerSocket>()
                .handle(SocketException::class.java)
                .withMaxAttempts(maxAttempts)
                .build(),
        ).get { _ ->
            createLocalTCPServerSocket(Random.nextInt(portFrom, portTo))
        }

    fun createLocalTCPServerSocket(port: Int): ServerSocket = ServerSocket(port, 20, InetAddress.getLoopbackAddress())

    fun createLocalTCPClientSocket(
        host: String? = null,
        port: Int = 0,
        maxAttempts: Int = 10,
    ): Socket =
        Failsafe.with(
            RetryPolicy.builder<Socket>()
                .handle(SocketException::class.java)
                .withMaxAttempts(maxAttempts)
                .onFailedAttempt {
                    log.warn { "Failed to connect to $host:$port (attempt ${it.attemptCount})" }
                }
                .withBackoff(Duration.ofSeconds(1), Duration.ofSeconds(30))
                .build(),
        ).get { _ ->
            Socket(host, port)
        }
}
