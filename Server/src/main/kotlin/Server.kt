import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.net.ServerSocket
import java.net.Socket

class Server(port: Int = 5804) {

    private fun log(message: Any?) = println("[SERVER] $message")
    private val maxConnections = 24
    private val availableClients = Channel<Client>(maxConnections)
    private val sSocket: ServerSocket = ServerSocket(port)
    private val clients = mutableListOf<Client>()
    private var running: Boolean = false


    fun getClientsCount(): Int {
        return clients.size
    }

    private val allMessageListeners = mutableListOf<(String) -> Unit>()
    fun addMessageListener(l: (String) -> Unit) {
        allMessageListeners.add(l)
    }

    fun removeMessageListener(l: (String) -> Unit) {
        allMessageListeners.remove(l)
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

    inner class Client(private val socket: Socket) {
        private fun log(message: Any?) = println("[CLIENT-$id] $message")
        private var connection: SocketIO? = null
        val id: Int = clients.size
        fun start() {
            connection = SocketIO(socket).apply {
                addSocketClosedListener {
                    clients.remove(this@Client)
                    log("Connection with client has been closed")
                }
                addMessageListener {
                    messageListeners.forEach { l ->
                        l(it)
                    }
                }
                addExceptionListener {
                    log("Exception: $it")
                    exceptionListeners.forEach { l -> l(it) }
                }
                communicate()
            }
        }

        fun stop() {
            log("Client has been stopped")
            connection?.stop()
        }

        private val messageListeners = mutableListOf<(String) -> Unit>()
        fun addMessageListener(l: (String) -> Unit) {
            messageListeners.add(l)
        }

        fun removeMessageListener(l: (String) -> Unit) {
            messageListeners.remove(l)
        }

        fun send(data: String) {
            connection?.send(data)
        }
    }

    /**
     * Заспрос к клиенту с ожиданием ответа
     * @param id id клиента
     * @param data данные запроса
     * @return ответ от клиента
     * */
    suspend fun request(id: Int, data: String): String? {

        var isCompleted = false // Получен ли результат
        var result: String? = null // Результат
        val onResponse: (String) -> Unit = {
            val message = Message.fromJson(it)
            if (message != null && message.id == id) {
                isCompleted = true
                result = message.data
            }
        }
        clients[id].addMessageListener(onResponse)
        send(id, Message(id, data).json())
        while (!isCompleted) {
            delay(10)
        }
        clients[id].removeMessageListener(onResponse)
        availableClients.send(clients[id])
        return result
    }

    private fun send(id: Int, data: String) {
        try {
            clients[id].send(data)
        } catch (e: Exception) {
            log(e.message)
        }
    }

    fun stop() {
        log("Server has been stopped")
        sSocket.close()
        running = false
    }

    fun start() {
        running = true
        log("Server started")
        allMessageListeners.forEach { l -> l("Server started") }
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
                            launch {
                                availableClients.send(client)
                            }
                        })
                }
            } catch (e: Exception) {
                allMessageListeners.forEach { l ->
                    e.message?.let { l(it) }
                }
            } finally {
                stopAllClients()
                sSocket.close()
                allMessageListeners.forEach { l ->
                    l("Сервер остановлен.")
                }
                log("Сервер остановлен")
            }
        }
    }

    private fun stopAllClients() {
        clients.forEach { client -> client.stop() }
    }

    suspend fun getAvailableClient(): Int {
        return availableClients.receive().id
    }
}