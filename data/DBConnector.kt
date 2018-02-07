package data

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create

class DBConnector {
    init {
        Database.connect(url, driver, user, pass)
        transaction { create(Users) }
    }

    companion object {
        private const val driver = "com.mysql.jdbc.Driver"
        private const val url = "jdbc:mysql://localhost:3306/messenger"
        private const val user = "ilya"
        private const val pass = "s"

        object Users : Table() {
            val id = integer("id").autoIncrement().primaryKey()
            val email = varchar("email", 50).uniqueIndex()
            val pass = varchar("pass", 32)
        }
    }
}