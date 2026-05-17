package pt.isel.pc.leic41n.sketches.apps.echoserver

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

private val logger = LoggerFactory.getLogger(AsynchronousEchoServer0::class.java)

suspend fun AsynchronousServerSocketChannel.acceptSuspend(): AsynchronousSocketChannel {
    val socket = suspendCoroutine { cont ->
        val handler = object : CompletionHandler<AsynchronousSocketChannel, Unit> {
            override fun completed(
                result: AsynchronousSocketChannel,
                attachment: Unit,
            ) {
                logger.info("CH completed called")
                cont.resume(result)
            }

            override fun failed(
                exc: Throwable,
                attachment: Unit,
            ) {
                logger.info("CH failed called")
                cont.resumeWithException(exc)
            }
        }
        this.accept(Unit, handler)
    }
    return socket
}

suspend fun AsynchronousSocketChannel.readSuspend(byteBuffer: ByteBuffer): Int {
    val socket = suspendCoroutine { cont ->
        val handler = object : CompletionHandler<Int, Unit> {
            override fun completed(
                result: Int,
                attachment: Unit,
            ) {
                logger.info("CH completed called")
                cont.resume(result)
            }

            override fun failed(
                exc: Throwable,
                attachment: Unit,
            ) {
                logger.info("CH failed called")
                cont.resumeWithException(exc)
            }
        }
        this.read(byteBuffer, Unit, handler)
    }
    return socket
}

suspend fun AsynchronousSocketChannel.writeSuspend(byteBuffer: ByteBuffer): Int {
    val socket = suspendCoroutine { cont ->
        val handler = object : CompletionHandler<Int, Unit> {
            override fun completed(
                result: Int,
                attachment: Unit,
            ) {
                logger.info("CH completed called")
                cont.resume(result)
            }

            override fun failed(
                exc: Throwable,
                attachment: Unit,
            ) {
                logger.info("CH failed called")
                cont.resumeWithException(exc)
            }
        }
        this.write(byteBuffer, Unit, handler)
    }
    return socket
}

fun main(args: Array<String>) {
    AsynchronousEchoServer0().run(InetSocketAddress("127.0.0.1", 8080))
}

class AsynchronousEchoServer0 {

    fun run(address: InetSocketAddress) {
        val serverSocket = AsynchronousServerSocketChannel.open()
        serverSocket.bind(address)
        runBlocking {
            while (true) {
                logger.info("Accepting socket...")
                val socket = serverSocket.acceptSuspend()
                logger.info("Socket accepted: {}", socket.remoteAddress)
                launch {
                    logger.info("Created new coroutine to handle connection")
                    while (true) {
                        logger.info("Reading ...")
                        val byteBuffer = ByteBuffer.allocate(4 * 1024)
                        socket.readSuspend(byteBuffer)
                        logger.info("Read completed")
                        byteBuffer.flip()
                        socket.writeSuspend(byteBuffer)
                        logger.info("Write completed")
                        byteBuffer.clear()
                    }
                }
            }
        }
    }
}
