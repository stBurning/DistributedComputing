package main.kotlin

import SocketIO
import java.net.Socket

class Client(
    host: String,
    port: Int
) {
    private val socket: Socket = Socket(host, port)
    private val communicator: SocketIO = SocketIO(socket)

    private val messageListeners = mutableListOf<(String) -> Unit>()
    fun addMessageListener(l: (String) -> Unit) {
        messageListeners.add(l)
    }
    fun removeMessageListener(l: (String) -> Unit) {
        messageListeners.remove(l)
    }

    private val exceptionListeners = mutableListOf<(String) -> Unit>()
    fun addExceptionListener(l: (String) -> Unit) {
        exceptionListeners.add(l)
    }
    fun removeExceptionListener(l: (String) -> Unit) {
        exceptionListeners.remove(l)
    }

    fun addSessionFinishedListener(l: () -> Unit) {
        communicator.addSocketClosedListener(l)
    }
    fun removeSessionFinishedListener(l: () -> Unit) {
        communicator.removeSocketClosedListener(l)
    }

    fun stop() {
        communicator.stop()
    }

    fun start() {
        communicator.addMessageListener {
            messageListeners.forEach { l -> l(it) }
        }
        communicator.addExceptionListener {
            exceptionListeners.forEach { l -> l(it) }
        }
        communicator.communicate()
    }

    fun send(data: String) {
        communicator.send(data)
    }


}