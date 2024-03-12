package com.faforever.gpgnet.io

import com.faforever.gpgnet.protocol.GpgnetMessage
import com.faforever.gpgnet.protocol.ReceivedMessage
import com.google.common.io.LittleEndianDataInputStream
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream

private val log = KotlinLogging.logger {}

/**
 * Not thread safe, as designed to be only accessed from a single thread
 */
class FaStreamReader(inputStream: InputStream) : Closeable {
    companion object {
        private const val MAX_CHUNK_SIZE = 10
    }

    private val objectLock = Object()

    @Volatile
    private var closing: Boolean = false
    private val inputStream = LittleEndianDataInputStream(BufferedInputStream(inputStream))

    init {
        log.debug { "FaStreamReader opened" }
    }

    private fun readString(): String {
        val size = inputStream.readInt()
        val buffer = ByteArray(size)
        inputStream.readFully(buffer)
        return String(buffer, FaStreamConstants.charset)
    }

    private fun readChunks(): List<Any> {
        val numberOfChunks = inputStream.readInt()

        if (numberOfChunks > MAX_CHUNK_SIZE) {
            throw IOException("Too many chunks: $numberOfChunks")
        }

        val chunks = mutableListOf<Any>()

        for (i in 1..numberOfChunks) {
            val data =
                when (inputStream.read()) {
                    FaStreamConstants.FieldTypes.INT -> inputStream.readInt()
                    else -> FaStreamConstants.parseString(readString())
                }

            chunks.add(data)
        }

        return chunks
    }

    @Throws(IOException::class)
    fun readMessage(): GpgnetMessage {
        val command = readString()
        val args = readChunks()

        val message = ReceivedMessage(command, args).tryParse()

        log.trace { "Received message: $message" }
        return message
    }

    @Throws(IOException::class)
    override fun close() {
        log.debug { "Closing FaStreamReader" }
        synchronized(objectLock) {
            closing = true
        }
        inputStream.close()
    }
}
