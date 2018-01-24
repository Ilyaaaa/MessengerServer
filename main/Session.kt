package main

import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.*
import java.net.Socket

class Session(private val server: Server, private val socket: Socket) : Runnable {
    private var stop = false
    private var writer = PrintWriter(socket.getOutputStream(), true)

    private val parser = JSONParser()

    override fun run() {
        try {
            outInfoMessage("Session started " + socket.inetAddress)

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            while (!stop) {
                val request = reader.readLine()
                if (request != null) {
                    println(request)
                    try {
                        val jsonObject = parser.parse(request) as JSONObject

                        when (jsonObject["id"].toString().toInt()) {
                            0 -> {

                            }

                            else -> {
                                outErrorMessage("Invalid message from client: $request")
                            }
                        }
                    }catch (ex: Exception) {
                        ex.printStackTrace()
                    }

                }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
            stop()
        }
    }

    fun sendMessage(message: String){
        writer.println(message)
    }

    fun stop() {
        stop = true

        try {
            socket.close()
            server.clients.remove(this)

            outInfoMessage("Client disconnected")
        } catch (ex: IOException) {
            outErrorMessage("Can't disconnect client")
        }

        outInfoMessage("Clients: " + server.clients.size)
    }
}
