package pt.isel.pc.leic41d.sketches.apps.echoserver

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
        private val logger = LoggerFactory.getLogger(EchoServer0::class.java)
    }

    fun run(address: InetSocketAddress) {
        val serverSocket = ServerSocket()
        serverSocket.bind(address)
        logger.info("Server is listening on {}", address)
        acceptClients(serverSocket)
    }

    private fun acceptClients(serverSocket: ServerSocket) {
        while (true) {
            logger.info("Accepting connection")
            val clientSocket: Socket = serverSocket.accept()
            logger.info("Client connected: {}", clientSocket.remoteSocketAddress)
            Thread.ofPlatform().start {
                echoLines(clientSocket)
            }
        }
    }

    private fun echoLines(clientSocket: Socket) {
        clientSocket.use {
            val reader = clientSocket.getInputStream().bufferedReader()
            val writer = clientSocket.getOutputStream().bufferedWriter()
            writer.writeLine("Welcome ${clientSocket.remoteSocketAddress}")
            while (true) {
                logger.info("Reading line...")
                val line: String? = reader.readLine()
                logger.info("Line received")
                if (line == null) {
                    logger.info("Client closed connection")
                    break
                }
                if (line == "exit") {
                    writer.writeLine("bye")
                    logger.info("Client exited")
                    break
                }
                writer.writeLine("server says: ${line.uppercase()}")
            }
        }
    }
}
