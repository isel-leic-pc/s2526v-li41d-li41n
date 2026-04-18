package pt.isel.pc.leic41n.sketches.apps.echoserver

import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

class NonBlockingEchoServer0 {

    companion object {
        private val logger = LoggerFactory.getLogger(NonBlockingEchoServer0::class.java)
    }

    fun run() {
        val nOfPorts = 10
        val selector = Selector.open()
        repeat(nOfPorts) {
            val serverSocket = ServerSocketChannel.open()
            serverSocket.configureBlocking(false)
            serverSocket.bind(InetSocketAddress("127.0.0.1", 8080 + it))
            serverSocket.register(selector, SelectionKey.OP_ACCEPT)
        }
        var lineCounter = 0
        while (true) {
            logger.info("Before select.")
            selector.select()
            val selectedKeys = selector.selectedKeys()
            val byteBuffer = ByteBuffer.allocate(1024)
            selectedKeys.forEach { selectedKey ->
                when (val channel = selectedKey.channel()) {
                    is ServerSocketChannel -> {
                        logger.info("Before accept.")
                        val socket: SocketChannel? = channel.accept()
                        logger.info("After accept.")
                        if (socket != null) {
                            socket.configureBlocking(false)
                            socket.register(selector, SelectionKey.OP_READ)
                        }
                    }

                    is SocketChannel -> {
                        byteBuffer.clear()
                        val counterBytes = "line $lineCounter: ".toByteArray()
                        lineCounter += 1
                        byteBuffer.put(counterBytes)
                        channel.read(byteBuffer)
                        byteBuffer.flip()
                        channel.write(byteBuffer)
                    }
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    NonBlockingEchoServer0().run()
}
