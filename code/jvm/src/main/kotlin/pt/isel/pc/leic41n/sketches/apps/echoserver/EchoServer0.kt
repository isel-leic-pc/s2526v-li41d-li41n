package pt.isel.pc.leic41n.sketches.apps.echoserver

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pt.isel.pc.utils.writeLine
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

fun main(args: Array<String>) {
    EchoServer0().run(
        InetSocketAddress("127.0.0.1", 8080),
    )
}

class EchoServer0 {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(EchoServer0::class.java)
    }

    fun run(address: InetSocketAddress) {
        val serverSocket = ServerSocket()
        serverSocket.bind(address)
        logger.info("Server is listening on {}", address)
        acceptClients(serverSocket)
    }

    private fun acceptClients(serverSocket: ServerSocket) {
        while (true) {
            logger.info("Accepting connection...")
            val socket: Socket = serverSocket.accept()
            logger.info("Connection accepted: {}", socket.remoteSocketAddress)
            echoLines(socket)
        }
    }

    private fun echoLines(socket: Socket) {
        socket.use {
            val reader = socket.getInputStream().bufferedReader()
            val writer = socket.getOutputStream().bufferedWriter()
            writer.writeLine("Welcome ${socket.remoteSocketAddress}")
            while (true) {
                logger.info("Waiting for line...")
                val line: String? = reader.readLine()
                logger.info("Line received: $line")
                if (line == null) {
                    logger.info("Connection ended")
                    break
                }
                if (line == "exit") {
                    writer.writeLine("bye.")
                    break
                }
                writer.writeLine("Server says: ${line.uppercase()}")
            }
        }
    }
}
