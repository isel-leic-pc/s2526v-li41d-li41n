package pt.isel.pc.leic41d.sketches.apps.echoserver

import org.slf4j.LoggerFactory
import pt.isel.pc.utils.writeLine
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Semaphore

fun main(args: Array<String>) {
    EchoServer0().run(
        InetSocketAddress("127.0.0.1", 8080),
    )
}

class EchoServer0 {

    companion object {
        private val logger = LoggerFactory.getLogger(EchoServer0::class.java)
    }

    private var messageCounter: Long = 0
    private val semaphore = Semaphore(2)

    fun run(address: InetSocketAddress) {
        val serverSocket = ServerSocket()
        serverSocket.bind(address)
        logger.info("Server is listening on {}", address)
        acceptClients(serverSocket)
    }

    private fun acceptClients(serverSocket: ServerSocket) {
        var nextClientId = 0
        while (true) {
            semaphore.acquire()
            logger.info("Accepting connection")
            val clientSocket: Socket = serverSocket.accept()
            logger.info("Client connected: {}", clientSocket.remoteSocketAddress)
            val connectionId = nextClientId
            nextClientId += 1
            Thread.ofPlatform().start {
                logger.info("Connection thread beginning for {}", connectionId)
                try {
                    echoLines(clientSocket, connectionId)
                } finally {
                    semaphore.release()
                }
            }
            logger.info("Started thread to handle new connection")
        }
    }

    private fun echoLines(
        clientSocket: Socket,
        connectionId: Int,
    ) {
        clientSocket.use {
            val reader = clientSocket.getInputStream().bufferedReader()
            val writer = clientSocket.getOutputStream().bufferedWriter()
            writer.writeLine("Welcome ${clientSocket.remoteSocketAddress}")
            var lineCounter = 0
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
                messageCounter += 1
                writer.writeLine("server says [$lineCounter]: ${line.uppercase()}")
                lineCounter += 1
            }
        }
    }
}
