package main

import data.DBConnector
import data.DBConnector.Companion as DataBase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.*
import java.net.Socket

class Session(private val server: Server, private val socket: Socket) : Runnable {
    private var stop = false
    private val writer = PrintWriter(socket.getOutputStream(), true)
    private val dbConnector = DBConnector()
    private val userInfo = User()

    private val parser = JSONParser()

    override fun run() {
        try {
            outInfoMessage("Session started " + socket.inetAddress)

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            while (!stop) {
                val request = reader.readLine()
                if (request == null){
                    stop()
                    break
                }

                println(request)
                try {
                    val jsonObject = parser.parse(request) as JSONObject

                    when (jsonObject["id"].toString().toInt()) {
                        0 -> {
                            val email = jsonObject["email"].toString()
                            val pass = jsonObject["pass"].toString()

                            var emailIsValid = false
                            var passIsValid = false

                            transaction {
                                for (user in DataBase.Users.select { DataBase.Users.email eq email })
                                    if (user[DataBase.Users.email] == email) {
                                        emailIsValid = true
                                        if (user[DataBase.Users.pass] == pass) {
                                            passIsValid = true

                                            userInfo.id = user[DataBase.Users.id]
                                            userInfo.email = email
                                            userInfo.pass = pass
                                        }

                                        break
                                    }
                            }

                            val resp = JSONObject()
                            resp.put("id", 0)
                            resp.put("emailIsValid", emailIsValid)
                            resp.put("passIsValid", passIsValid)

                            sendMessage(resp.toString())
                        }

                        1 -> {
                            stop()
                        }

                        else -> {
                            outErrorMessage("Invalid message from client: $request")
                        }
                    }
                }catch (ex: Exception) {
                    ex.printStackTrace()
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

    companion object {
        private class User{
            var id = -1
            var email = String()
            var pass = String()
        }
    }
}
