package main.enums

enum class ServerErrors(val errorMessage: String) {
    INV_COMMAND("Invalid command"),
    INV_SYNTAX("Invalid command syntax"),
    INV_ARGS("Invalid arguments"),
    NOT_RUNNING_ERROR("Server not running"),
    ALREADY_RUNNING_ERROR("Server already running"),
    DB_CONNECT_ERROR("Can't connect to data base")
}