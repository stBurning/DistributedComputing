import services.DBService
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom

fun log(message: Any?) = println("[${Thread.currentThread().name}] $message")

fun main() = runBlocking {
    val server = Server()
    val dbService = DBService("matrices")
    server.start()

    var cmd: String
    val sc = Scanner(System.`in`)

    fun compute() = runBlocking{
        log("Distributed computing started")
        val m1 = dbService.getMatrix("matrices", 1)
        val m2 = dbService.getMatrix("matrices", 2)
        log("Multiplication between two matrices N[${m1.rows},${m1.cols}] and M[${m2.rows},${m2.cols}]")
        val validCount = 2 // Сколько раз будет пересчитываться операция
        val workersCount = server.getClientsCount()

        for (row in 0 until m1.rows) {
            val part = createDoubleMatrix(m1.cols, 1) { k, _ -> m1[k, row] }
            for (i in 0 until validCount) {
                launch {
                    log("Computation: part $row, val $i started")
                    val workerId = (row + i)%workersCount
                    server.send(workerId, Message(workerId,validCount*row + i, "${part.toJson()}|${m2.toJson()}").json())
                    server.addMessageListener {
                        val message = Message.fromJson(it)
                        if(message?.id == workerId && message.part == validCount*row + i){
                            log("TaskResult: $message")
                            doubleMatrixFromJson(message.data)
                        }
                    }
                }
            }
        }
    }

    do {
        cmd = sc.nextLine()
        CoroutineScope(Dispatchers.IO).launch{
            dbService.clearTable("temp")
            compute()
        }

    } while (cmd != "\\stop")
    server.stop()
}





