package pt.isel.pc.leic41d.sketches.apps.echoserver

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val logger = LoggerFactory.getLogger(AsynchronousEchoServer::class.java)

suspend fun AsynchronousServerSocketChannel.acceptSuspend(): AsynchronousSocketChannel {
    val acceptedSocket: AsynchronousSocketChannel = suspendCoroutine { continuation ->
        this.accept(
            Unit,
            object : CompletionHandler<AsynchronousSocketChannel, Unit> {
                override fun completed(
                    result: AsynchronousSocketChannel,
                    attachment: Unit,
                ) {
                    logger.info("CH.completed called, resuming continuation")
                    continuation.resume(result)
                }

                override fun failed(
                    exc: Throwable,
                    attachment: Unit,
                ) {
                    logger.info("CH.failed called, resuming continuation")
                    continuation.resumeWithException(exc)
                }
            },
        )
    }
    return acceptedSocket
}

suspend fun AsynchronousSocketChannel.readSuspend(buffer: ByteBuffer): Int {
    val bytesRead = suspendCoroutine { continuation ->
        this.read(
            buffer,
            Unit,
            object : CompletionHandler<Int, Unit> {
                override fun completed(
                    result: Int,
                    attachment: Unit,
                ) {
                    logger.info("read CH.completed called, resuming continuation")
                    continuation.resume(result)
                }

                override fun failed(
                    exc: Throwable,
                    attachment: Unit,
                ) {
                    logger.info("read CH.failed called, resuming continuation")
                    continuation.resumeWithException(exc)
                }
            },
        )
    }
    return bytesRead
}

suspend fun AsynchronousSocketChannel.writeSuspend(buffer: ByteBuffer): Int {
    val bytesWritten = suspendCoroutine { continuation ->
        this.write(
            buffer,
            Unit,
            object : CompletionHandler<Int, Unit> {
                override fun completed(
                    result: Int,
                    attachment: Unit,
                ) {
                    logger.info("write CH.completed called, resuming continuation")
                    continuation.resume(result)
                }

                override fun failed(
                    exc: Throwable,
                    attachment: Unit,
                ) {
                    logger.info("write CH.failed called, resuming continuation")
                    continuation.resumeWithException(exc)
                }
            },
        )
    }
    return bytesWritten
}

class AsynchronousEchoServer {

    fun run(address: InetSocketAddress) {
        runBlocking {
            val serverSocket = AsynchronousServerSocketChannel.open()
            serverSocket.bind(address)
            var nextId = 0
            while (true) {
                logger.info("Accepting new socket...")
                val socket = serverSocket.acceptSuspend()
                logger.info("Socket accepted from {}", socket.remoteAddress)
                val id = nextId++
                launch {
                    val byteBuffer = ByteBuffer.allocate(4 * 1024)
                    while (true) {
                        val len = socket.readSuspend(byteBuffer)
                        logger.info("{}: Read {} bytes", id, len)
                        byteBuffer.flip()
                        socket.writeSuspend(byteBuffer)
                        byteBuffer.clear()
                    }
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    AsynchronousEchoServer().run(InetSocketAddress("127.0.0.1", 8080))
}
