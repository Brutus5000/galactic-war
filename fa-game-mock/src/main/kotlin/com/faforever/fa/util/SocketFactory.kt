package com.faforever.fa.util

import dev.failsafe.Failsafe
import dev.failsafe.RetryPolicy
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketException
import kotlin.random.Random

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

    fun createLocalTCPSocket(
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
            createLocalTCPSocket(Random.nextInt(portFrom, portTo))
        }

    fun createLocalTCPSocket(port: Int): ServerSocket = ServerSocket(port, 20, InetAddress.getLoopbackAddress())
}
