package main

import data.Settings
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors


class Server : Runnable {
    private lateinit var serverSocket: ServerSocket
    var clients = CopyOnWriteArrayList<Session>()
    var isRunning = false

    override fun run() {
        try {
            serverSocket = ServerSocket(Settings.port)
            outInfoMessage("Server started. Port: " + Settings.port)

            val executorService = Executors.newCachedThreadPool()
            isRunning = true

            while (!serverSocket.isClosed) {
                try {
                    outInfoMessage("Clients: " + clients.size)
                    val socket = serverSocket.accept()
                    outInfoMessage("New client connected")
                    val session = Session(this, socket)
                    clients.add(session)
                    executorService.submit(session)
                } catch (ex: IOException) {
                    if (!serverSocket.isClosed) {
                        try {
                            serverSocket.close()
                        } catch (ex: IOException) {
                            ex.printStackTrace()
                        }
                    }
                }

            }
        } catch (ex: IOException) {
            outErrorMessage("Can't connect to " + Settings.port + " port")
        }
    }

    fun stop() {
        for (client in clients)
            client.stop()

        try {
            serverSocket.close()

            isRunning = false
            outInfoMessage("Server stopped")
        } catch (ex: IOException) {
            outErrorMessage("Can't stop server")
        }
    }
}
