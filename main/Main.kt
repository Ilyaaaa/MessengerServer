package main

import data.Settings
import main.enums.ServerErrors
import java.util.*

private val server: Server = Server()
private val settings: Settings = Settings()

fun main(args: Array<String>) {
    val input = Scanner(System.`in`)

    var text: String
    var command: String
    var commandArgs: String

    outInfoMessage("Input 'run' to run server, 'help' to open manual")
    while (true) {
        text = input.nextLine()
        val n = text.indexOf(" ")
        if (n == -1) {
            command = text
            commandArgs = ""
        } else {
            command = text.substring(0, n)
            commandArgs = text.substring(n)

            while (commandArgs.startsWith(" "))
                commandArgs = commandArgs.substring(1)
            while (commandArgs.endsWith(" "))
                commandArgs = commandArgs.substring(0, commandArgs.length - 1)
        }

        if (commandArgs == "") {
            when (command) {
                "run" -> {
                    if (!server.isRunning)
                        runServer()
                    else
                        outErrorMessage(ServerErrors.ALREADY_RUNNING_ERROR.errorMessage)
                }
                "stop" -> {
                    if (server.isRunning)
                        server.stop()
                    else
                        outErrorMessage(ServerErrors.NOT_RUNNING_ERROR.errorMessage)
                }

                "restart" -> {
                    if (server.isRunning) {
                        server.stop()
                        runServer()
                    } else
                        outErrorMessage(ServerErrors.NOT_RUNNING_ERROR.errorMessage)
                }
                "close" -> System.exit(0)

                "help" -> println(getManual())

                else -> outErrorMessage(ServerErrors.INV_SYNTAX.errorMessage)
            }
        } else {
            when (command) {
                "send" -> send(commandArgs)

                "set" -> set(commandArgs)

                else -> outErrorMessage(ServerErrors.INV_COMMAND.errorMessage)
            }
        }
    }
}

fun Boolean.toInt() = if (this) 1 else 0

private fun send(args: String) {
    var args = args
    if (server.isRunning) {
        val n = args.indexOf(" ")
        if (n != -1) {
            val command = args.substring(0, n)
            args = args.substring(n + 1)

            when (command) {
                "all" -> for (client in server.clients) {
                    client.sendMessage(args)
                }

                else -> outErrorMessage(ServerErrors.INV_SYNTAX.errorMessage)
            }
        } else
            outErrorMessage(ServerErrors.INV_SYNTAX.errorMessage)
    } else
        outErrorMessage(ServerErrors.NOT_RUNNING_ERROR.errorMessage)
}

private fun set(args: String) {
    var args = args
    val n = args.indexOf(" ")
    if (n != -1) {
        val command = args.substring(0, n)
        args = args.substring(n + 1)

        when (command) {
            "port" -> try {
                settings.setPort(Integer.parseInt(args))
            } catch (ex: NumberFormatException) {
                outErrorMessage(ServerErrors.INV_ARGS.errorMessage)
            }

            else -> outErrorMessage(ServerErrors.INV_SYNTAX.errorMessage)
        }
    } else
        outErrorMessage(ServerErrors.INV_SYNTAX.errorMessage)
}

private fun runServer() {
    Thread(server).start()
}

private fun getManual(): String {
    val builder = StringBuilder()
            .append("run - Run server\n")
            .append("stop - Stop server\n")
            .append("restart - Restart server\n")
            .append("send [to] [message] - Send message\n")
            .append("set [option] [value] <value2 ...>\n")
            .append("close - Exit\n")

    return builder.toString()
}

fun outErrorMessage(message: String) {
    println("[Error] " + message)
}

fun outInfoMessage(message: String) {
    println("[Info] " + message)
}