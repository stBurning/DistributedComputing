import kotlinx.coroutines.*
import java.net.ServerSocket
import java.net.Socket

class Server(port: Int = 5804) {

    private fun log(message: Any?) = println("[SERVER] $message")

    private val sSocket: ServerSocket = ServerSocket(port)
    private val clients = mutableListOf<Client>()
    private var running: Boolean


    fun getClientsCount(): Int {
        return clients.size
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

    private val connectionListeners = mutableListOf<(Int) -> Unit>()
    fun addConnectionListener(l: (Int) -> Unit) {
        connectionListeners.add(l)
    }

    fun removeConnectionListener(l: (Int) -> Unit) {
        connectionListeners.remove(l)
    }

    init {
        running = false
    }

    inner class Client(private val socket: Socket) {
        private fun log(message: Any?) = println("[CLIENT-$id] $message")
        private var connection: SocketIO? = null
        var available = true
        val id: Int = clients.size
        fun start() {
            connection = SocketIO(socket).apply {
                addSocketClosedListener {
                    clients.remove(this@Client)
                    log("Connection with client has been closed")
                }
                addMessageListener {
                    //log("Message: $it")
                    messageListeners.forEach { l -> l(it)
                        available = true
                    }
                }
                addExceptionListener {
                    log("Exception: $it")
                    exceptionListeners.forEach { l ->
                        l(it)
                    }
                }
                communicate()
            }
        }

        fun stop() {
            log("Client has been stopped")
            connection?.stop()
        }

        fun send(data: String) {
            connection?.send(data)
        }
    }

    fun send(id: Int, data: String){
        try{ clients[id].send(data) }
        catch (e: Exception){ log(e.message) }
    }

    fun stop() {
        log("Server has been stopped")
        sSocket.close()
        running = false
    }

    fun start(){
        running = true
        log("Server started")
        messageListeners.forEach { l -> l("Server started") }
        log("Waiting for connection")
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            try {
                while (running) {
                    clients.add(
                        Client(
                            sSocket.accept()
                        ).also { client ->
                            client.start()
                            connectionListeners.forEach { it(client.id) }
                            log("Client[${client.id}] connected")
                        })
                }
            } catch (e: Exception) {
                messageListeners.forEach { l ->
                    e.message?.let { l(it) }
                }
            } finally {
                stopAllClients()
                sSocket.close()
                messageListeners.forEach { l ->
                    l("Сервер остановлен.")
                }
                log("Сервер остановлен")
            }
        }
    }

    private fun stopAllClients() {
        clients.forEach { client -> client.stop() }
    }
}