import kotlinx.coroutines.*
import services.DBService
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.ArrayList

fun log(message: Any?) = println("[SERVER_MAIN] $message")

fun main() = runBlocking {
    val server = Server()
    val dbService = DBService("matrices")
    server.start()

    var cmd: String
    val sc = Scanner(System.`in`)
    /**Запуск распределенных вычислений
     * @param matrixId идентификатор для результирующей матрицы в БД
     * @param id1 id левой матрицы в БД
     * @param id2 id правой матрицы в БД
     * @param validCount кол-во итераций для валидации
     * */
    fun compute(id1: Int, id2: Int,matrixId: Int, validCount: Int) = runBlocking {
        log("Распределенные вычисления запущенны")
        val m1 = dbService.getMatrix("matrices", id1)
        val m2 = dbService.getMatrix("matrices", id2)
        log("Умножение матриц N[${m1.rows},${m1.cols}] and M[${m2.rows},${m2.cols}]")

        for (row in 0 until m1.rows) {
            val part = createDoubleMatrix(m1.cols, 1) { k, _ -> m1[k, row] } // Часть матрицы для отдельного вычисления
            launch(Dispatchers.IO) {
                var success = false
                while (!success) {
                    val jobs = ArrayList<Job>(validCount)
                    val results = mutableListOf<String>()
                    /**Вычисление задачи
                     * @param part часть матирицы для вычислений
                     * @return корутина вычисления*/
                    fun computePart(part: DoubleMatrix): Job {
                        return launch {
                            log("Ожидание свободного подключения")
                            val client = server.getAvailableClient() // Получение свободного подключения
                            log("Получен доступ к клиенту №$client")
                            log("Запрос к клиенту №$client\" отправлен")
                            // Запрос и ожидание результата
                            val response = server.request(client, "${part.toJson()}|${m2.toJson()}")
                            if (response != null) {
                                log("Резульат от клиента №$client\": $response")
                                results.add(response)
                            } else {
                                throw Exception("Не удалось получить результат")
                            }
                        }
                    }
                    for (i in 0 until validCount) {
                        val job = computePart(part)
                        jobs.add(i, job)
                    }
                    jobs.forEach { job ->job.join() }
                    val match = results // Валидация результатов
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
            compute(0, 1, lastMatrixID + 1, 2)
            println("Вычисления завершены")
            log("Результат вычислений:")
            println(dbService.getMatrix("matrices", lastMatrixID + 1))
        }
    } while (cmd != "\\stop")
    server.stop()
}





