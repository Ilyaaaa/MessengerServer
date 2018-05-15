package main

import data.DBConnector
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import data.DBConnector.Companion as DataBase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.simple.JSONArray
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
                if (request == null) {
                    stop()
                    break
                }

                println(request)
                try {
                    val jsonObject = parser.parse(request) as JSONObject

                    when (jsonObject["id"].toString().toInt()) {
                    //authorisation
                        0 -> {
                            val email = jsonObject["email"].toString()
                            val pass = jsonObject["pass"].toString()

                            authorize(email, pass, 0)
                        }

                    //exit
                        1 -> {
                            stop()
                        }

                    //register
                        2 -> {
                            val email = jsonObject["email"].toString()
                            val pass = jsonObject["pass"].toString()
                            val login = jsonObject["name"].toString()
                            val name = jsonObject["name"].toString()
                            val name2 = jsonObject["name2"].toString()


                            val resp = JSONObject()
                            resp.put("id", 1)

                            var emailIsValid = true
                            var loginIsValid = true
                            transaction {
                                DataBase.Users.select { (DataBase.Users.email eq email) or (DataBase.Users.login eq login) }.forEach {
                                    if (it[DataBase.Users.email] == email) emailIsValid = false
                                    else if (it[DataBase.Users.login] == login) loginIsValid = false
                                }

                                if (emailIsValid && loginIsValid) {
                                    DataBase.Users.insert {
                                        it[DataBase.Users.email] = email
                                        it[DataBase.Users.login] = login
                                        it[DataBase.Users.name] = name
                                        it[DataBase.Users.name2] = name2
                                        it[DataBase.Users.pass] = pass
                                    }

                                }
                            }

                            resp.put("emailIsValid", emailIsValid)
                            resp.put("loginIsValid", loginIsValid)

                            sendMessage(resp.toString())
                        }

                        //authorisation check
                        3 -> {
                            val email = jsonObject["email"].toString()
                            val pass = jsonObject["pass"].toString()

                            authorize(email, pass, 2)
                        }

                        //search users
                        4 -> {
                            val query = jsonObject["query"].toString()

                            val resp = JSONObject()
                            resp.put("id", 4)
                            val data = JSONArray()
                            transaction {
                                DataBase.Users.select{
                                    (DataBase.Users.login like "%$query%") or (DataBase.Users.name like "%$query%") or (DataBase.Users.name2 like "%$query%")
                                }.forEach {
                                    val user = JSONObject()
                                    user.put("id", it[DataBase.Users.id])
                                    user.put("email", it[DataBase.Users.email])
                                    user.put("login", it[DataBase.Users.login])
                                    user.put("name", it[DataBase.Users.name])
                                    user.put("name2", it[DataBase.Users.name2])
                                    data.add(user)
                                }
                            }
                            resp.put("data", data.toString())
                            sendMessage(resp.toString())
                        }

                        5 -> {
                            val usersId = jsonObject["usersId"] as JSONArray

                            val resp = JSONObject()
                            resp.put("id", 5)
                            val data = JSONArray()
                            transaction {
                                DataBase.Users.select{DataBase.Users.id inList usersId}.forEach{
                                    val user = JSONObject()
                                    user.put("id", it[DataBase.Users.id])
                                    user.put("email", it[DataBase.Users.email])
                                    user.put("login", it[DataBase.Users.login])
                                    user.put("name", it[DataBase.Users.name])
                                    user.put("name2", it[DataBase.Users.name2])
                                    data.add(user)
                                }
                            }
                            resp.put("data", data.toString())
                            sendMessage(resp.toString())
                        }

                        6 -> {
                            var result = false

                            transaction {
                                val chatId = DataBase.Chats.insert {
                                    it[DataBase.Chats.name] = jsonObject["name"].toString()
                                    it[DataBase.Chats.description] = jsonObject["description"].toString()
                                    it[DataBase.Chats.ownerId] = jsonObject["ownerId"].toString().toInt()
                                }.generatedKey

                                if (chatId == null) return@transaction

                                val usersId = ArrayList<Int>()
                                for (userId in jsonObject["users"] as JSONArray) usersId.add(userId.toString().toInt())

                                DataBase.ChatUsers.batchInsert(usersId) { userId ->
                                    run {
                                        this[DataBase.ChatUsers.userId] = userId
                                        this[DataBase.ChatUsers.chatId] = chatId.toInt()
                                    }
                                }

                                result = true
                            }

                            val resp = JSONObject()
                            resp.put("id", 6)
                            resp.put("success", result)
                            sendMessage(resp.toString())
                        }

                        else -> {
                            outErrorMessage("Invalid message from client: $request")
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            stop()
        }
    }

    private fun authorize(email: String, pass: String, respMsgId: Int) {
        var emailIsValid = false
        var passIsValid = false

        transaction {
            DataBase.Users.select { DataBase.Users.email eq email }.forEach {
                if (it[DataBase.Users.email] == email) {
                    emailIsValid = true
                    if (it[DataBase.Users.pass] == pass) {
                        passIsValid = true

                        userInfo.id = it[DataBase.Users.id]
                        userInfo.email = email
                        userInfo.login = it[DataBase.Users.login]
                        userInfo.pass = pass
                        userInfo.name = it[DataBase.Users.name]
                        userInfo.name2 = it[DataBase.Users.name2]
                    }
                }
            }
        }

        val resp = JSONObject()
        resp.put("id", respMsgId)
        resp.put("emailIsValid", emailIsValid)
        resp.put("passIsValid", passIsValid)
        resp.put("login", userInfo.login)
        resp.put("name", userInfo.name)
        resp.put("name2", userInfo.name2)
        resp.put("userId", userInfo.id)

        sendMessage(resp.toString())
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
            var login = String()
            var name = String()
            var name2 = String()
            var pass = String()
        }
    }
}
