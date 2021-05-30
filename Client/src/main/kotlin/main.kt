package main.kotlin

import Message
import doubleMatrixFromJson
import x
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.system.exitProcess


fun main() {
    val client = Client("localhost", 5804)
    client.start()

    client.addMessageListener { data ->
        val message = Message.fromJson(data)
        if (message != null) {
            val request = message.data.split('|')
            val id = message.id
            val part = message.part
            val m1 = doubleMatrixFromJson(request[0])
            val m2 = doubleMatrixFromJson(request[1])
            if (m1 != null && m2 != null) {
                val result = (m1.x(m2)).toJson()
                println("[$id, $part] Отправил: $result")
                client.send(Message(id, part, result).json())
            }
        }

    }

    client.addExceptionListener { e ->
        println("[Exception] $e")
        exitProcess(0)
    }

    client.addSessionFinishedListener {
        println("Session Finished!")
        exitProcess(0)
    }
    while (true) {
        val scanner = Scanner(System.`in`)
        client.send(scanner.nextLine())
    }
}






