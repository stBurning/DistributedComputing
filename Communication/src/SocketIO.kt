import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class SocketIO(private val socket: Socket) {

    private var running = true

    private val socketClosedListener = mutableListOf<() -> Unit>()
    fun addSocketClosedListener(l: () -> Unit) {
        socketClosedListener.add(l)
    }

    fun removeSocketClosedListener(l: () -> Unit) {
        socketClosedListener.remove(l)
    }

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


    fun stop() {
        running = false
        socket.close()
    }

    fun communicate() {
        running = true
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                while (running) {
                    val data = reader.readLine()
                    if (data != null)
                        messageListeners.forEach { l -> l(data) }
                    else {
                        throw IOException("[SocketIO] Connection Failed!")
                    }
                }
            } catch (ex: Exception) {
                exceptionListeners.forEach { l -> ex.message?.let { l(it) } }
            } finally {
                socket.close()
                socketClosedListener.forEach { it() }
            }
        }
    }

    fun send(data: String): Boolean {
        return try {
            val writer = PrintWriter(socket.getOutputStream())
            writer.println(data)
            writer.flush()
            true
        } catch (ex: Exception) {
            log(ex.message)
            false
        }
    }

    private fun log(message: String?) {
        println("[SocketIO] $message")
    }


}