package pt.isel.pc.leic41d.sketches.apps.echoserver

import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

class EchoServerNonBlocking0 {

    companion object {
        private val logger = LoggerFactory.getLogger(EchoServerNonBlocking0::class.java)
    }

    class ConnectionState(
        var lineCounter: Int = 0,
    )

    fun run() {
        val selector = Selector.open()
        val nOfServerSockets = 10
        val serverSockets = List(nOfServerSockets) {
            ServerSocketChannel.open()
        }
        serverSockets.forEachIndexed { index, channel ->
            channel.bind(InetSocketAddress("127.0.0.1", 8080 + index))
            channel.configureBlocking(false)
            channel.register(selector, SelectionKey.OP_ACCEPT)
        }
        val byteBuffer = ByteBuffer.allocate(1024)
        while (true) {
            logger.info("Waiting on selector.select.")
            selector.select()
            logger.info("Select returned.")
            val selectedKeys: Set<SelectionKey> = selector.selectedKeys()
            selectedKeys.forEach { selectionKey ->
                when (val channel = selectionKey.channel()) {
                    is ServerSocketChannel -> {
                        logger.info("Accepting socket from ServerSocketChannel")
                        val socket: SocketChannel? = channel.accept()
                        if (socket != null) {
                            logger.info("SocketChannel accepted.")
                            socket.configureBlocking(false)
                            val connectionState = ConnectionState()
                            socket.register(selector, SelectionKey.OP_READ, connectionState)
                        }
                    }
                    is SocketChannel -> {
                        val connectionState = selectionKey.attachment() as ConnectionState
                        val counterBytes = "[${connectionState.lineCounter}]".encodeToByteArray()
                        connectionState.lineCounter += 1
                        logger.info("Reading from socket...")
                        byteBuffer.clear()
                        byteBuffer.put(counterBytes)
                        val readBytes = channel.read(byteBuffer)
                        logger.info("Read {}", readBytes)
                        byteBuffer.flip()
                        logger.info("Writing into socket...")
                        channel.write(byteBuffer)
                    }
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    EchoServerNonBlocking0().run()
}
