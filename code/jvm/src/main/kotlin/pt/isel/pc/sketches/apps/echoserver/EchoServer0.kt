package pt.isel.pc.sketches.apps.echoserver

import org.slf4j.LoggerFactory
import pt.isel.pc.utils.writeLine
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

fun main(args: Array<String>) {
    EchoServer0().run("127.0.0.1", 8080)
}

class EchoServer0 {

    companion object {
        private val logger = LoggerFactory.getLogger(EchoServer0::class.java)
    }

    fun run(
        ipAddress: String,
        port: Int,
    ) {
        // Create the ServerSocket and bind it to an IP and port
        val serverSocket = ServerSocket()
        serverSocket.bind(InetSocketAddress(ipAddress, port))
        logger.info("ServerSocket bound to {}:{}", ipAddress, port)
        acceptClients(serverSocket)
    }

    private fun acceptClients(serverSocket: ServerSocket) {
        while (true) {
            logger.info("Accepting client...")
            val clientSocket = serverSocket.accept()
            logger.info("Client accepted from {}", clientSocket.remoteSocketAddress)
            echoLines(clientSocket)
        }
    }

    private fun echoLines(clientSocket: Socket) {
        clientSocket.use {
            clientSocket.inputStream.bufferedReader().use { reader ->
                clientSocket.outputStream.bufferedWriter().use { writer ->
                    try {
                        writer.writeLine("Welcome!")
                        while (true) {
                            val line = reader.readLine()
                            if (line == null) {
                                logger.info("client socket is closed, ending echo loop")
                                return
                            }
                            if (line == "exit") {
                                logger.info("client wants to exit, ending echo loop")
                                return
                            }
                            logger.info("Received line: {}", line)
                            writer.writeLine(line.uppercase())
                        }
                    } catch (ex: IOException) {
                        logger.info("Exception while reading or writing from socket, ending client")
                        // continue without re-throwing
                    }
                }
            }
        }
    }
}
