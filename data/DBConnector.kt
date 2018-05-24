package data

import main.enums.ServerErrors
import main.stopServer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create

class DBConnector {
    init {
        Database.connect(url, driver, user, pass)
        try {
            transaction { create(Users, Chats, ChatUsers, Messages) }
        }catch (ex: ExceptionInInitializerError){
            println("${ServerErrors.DB_CONNECT_ERROR.errorMessage}: $ex")
            stopServer()
        }
    }

    companion object {
        private const val driver = "com.mysql.jdbc.Driver"
        private const val url = "jdbc:mysql://localhost:3306/messenger"
        private const val user = "ilya"
        private const val pass = "s"

        object Users: Table() {
            val id = integer("id").autoIncrement().primaryKey()
            val email = varchar("email", 50).uniqueIndex()
            val login = varchar("login", 50).uniqueIndex()
            val name = varchar("name", 50)
            val name2 = varchar("name2", 50)
            val pass = varchar("pass", 32)
        }

        object Chats: Table() {
            val id = integer("id").autoIncrement().primaryKey()
            val name = varchar("name", 50)
            val description = text("description").nullable()
            val ownerId = integer("ownerId") references Users.id
         }

        object ChatUsers: Table() {
            val id = integer("id").autoIncrement().primaryKey()
            val userId = integer("userId") references Users.id
            val chatId = integer("chatId") references Chats.id
        }

        object Messages: Table() {
            val id = integer("id").autoIncrement().primaryKey()
            val text = text("text")
            val senderId = integer("senderId") references Users.id
            val chatId = integer("chatId") references Chats.id
            val sendTime = long("sendTime")
        }
    }
}