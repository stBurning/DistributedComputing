import kotlinx.coroutines.*
import services.DBService
import java.util.*
import kotlin.collections.ArrayList

fun log(message: Any?) = println("[SERVER_MAIN] $message")

fun main() = runBlocking {
    val server = Server()
    val dbService = DBService("matrices")
    server.start()

    var cmd: String // Командная строка
    val sc = Scanner(System.`in`)

    fun compute(matrixId: Int) = runBlocking {
        log("Распределенные вычисления запущенны")
        val m1 = dbService.getMatrix("matrices", 1)
        val m2 = dbService.getMatrix("matrices", 2)
        log("Умножение матриц N[${m1.rows},${m1.cols}] and M[${m2.rows},${m2.cols}]")
        val validCount = 2 // Сколько раз будет пересчитываться операция

        for (row in 0 until m1.rows) {
            val part = createDoubleMatrix(m1.cols, 1) { k, _ -> m1[k, row] }
            launch(Dispatchers.IO) {
                var success = false
                while (!success) {
                    val jobs = ArrayList<Job>(validCount)
                    val results = mutableListOf<String>()
                    for (i in 0 until validCount) {
                        val job = launch {
                            log("Ожидание свободного подключения")
                            val client = server.getAvailableClient()
                            log("Получен доступ к клиенту №$client")
                            log("Запрос к клиенту №$client\" отправлен")
                            val response = server.request(client, "${part.toJson()}|${m2.toJson()}")
                            if (response != null) {
                                log("Резульат от клиента №$client\": $response")
                                results.add(response)
                            } else {
                                throw Exception("Не удалось получить результат")
                            }

                        }
                        jobs.add(i, job)
                    }
                    jobs.forEach { job ->job.join() }
                    val match = results
                        .stream()
                        .allMatch { x -> x == results[0] }
                    if (match) {
                        log("Результат получен успешно: ${results[0]}")
                        val matrix = doubleMatrixFromJson(results[0])
                        if (matrix != null){
                            dbService.addRow("matrices", matrixId ,row, matrix)
                        }else{
                            log("Не удалось получить результат")
                        }


                        success = true
                    }
                    results.clear()
                    jobs.clear()
                }

            }
        }
    }

    do {
        cmd = sc.nextLine()
        val lastMatrixID = dbService.getMaxId("matrices")
        CoroutineScope(Dispatchers.IO).launch {
            compute(lastMatrixID + 1)
            println("Вычисления завершены")
            log("Результат вычислений:")
            println(dbService.getMatrix("matrices", lastMatrixID + 1))
        }
    } while (cmd != "\\stop")
    server.stop()
}





