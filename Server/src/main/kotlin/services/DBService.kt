package services

import DoubleMatrix
import createDoubleMatrix
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException


class DBService(
    private val dbName: String,
    private val address: String = "localhost",
    private val port: Int = 3306,
    private val user: String = "root",
    private val password: String = "root"
) {
    private var connection: Connection? = null

    init {
        connection = DriverManager.getConnection(
            "jdbc:mysql://$address:$port/$dbName?serverTimezone=UTC", user, password
        )

    }

    fun dropTable(table: String) {
        val statement = connection?.createStatement()
        statement?.execute("DROP TABLE `matrices`.`${table}`")
    }

    private fun getMatrixInfo(table: String, id: Int): Pair<Int, Int> {
        val statement = connection?.createStatement()
        val response = statement?.executeQuery(
            "SELECT COUNT(DISTINCT `row`) as `row_count`, COUNT(DISTINCT `col`) as `col_count` " +
                    "FROM `${table}` WHERE `${table}`.`id` = $id"
        )
        if (response?.next() == true) {
            val rowCount = response.getInt("row_count")
            val colCount = response.getInt("col_count")
            return Pair(rowCount, colCount)
        } else {
            throw Exception("Couldn't get information about this matrix")
        }
    }

    fun clearTable(name: String) {
        val statement = connection?.createStatement()
        statement?.execute("TRUNCATE `matrices`.`${name}`")
    }

    /**
     * Получение матрицы из базы данных
     * @param table название таблицы в базе данных
     * @param id номер матрицы в таблице
     * */
    fun getMatrix(table: String, id: Int): DoubleMatrix {
        val meta = getMatrixInfo(table, id)
        val s = connection?.createStatement()
        val idQuery = "SELECT `row`,`col`,`value` FROM `${table}` WHERE ${table}.id = " +
                "\"${id}\""
        val response = s?.executeQuery(idQuery)
        val matrix = createDoubleMatrix(meta.first, meta.second) { _, _ -> 0.0 }
        while (response?.next() == true) {
            matrix[response.getInt("row"),
                    response.getInt("col")] = response.getDouble("value")
        }
        return matrix
    }

    fun addMatrix(table: String, id: Int, matrix: DoubleMatrix) {
        val s = connection?.createStatement()
        for (i in 0 until matrix.rows) {
            for (j in 0 until matrix.cols) {
                s?.addBatch(
                    "INSERT INTO `${table}` (`id`, `row`, `col`, `value`) " +
                            "VALUES ('$id', '$i', '$j', '${matrix[i, j]}');"
                )
            }
        }
        s?.executeBatch()
        s?.clearBatch()
    }


    /**Функция, создающая таблицы в базе данных на основе SQL-дампа
     * @param path путь до SQL-дампа*/
    fun createDataBaseFromDump(path: String) {
        println("Создание структуры базы данных из дампа...")
        try {
            val s = connection?.createStatement()
            var query = ""
            File(path).forEachLine {
                if (!it.startsWith("--") && it.isNotEmpty()) {
                    query += it;
                    if (it.endsWith(';')) {
                        s?.addBatch(query)
                        query = ""
                    }
                }
            }
            s?.executeBatch()
            println("Структура базы данных успешно создана.")
        } catch (e: SQLException) {
            println(e.message)
        } catch (e: Exception) {
            println(e.message)
        }
    }

    /**Функция для заполнения таблицы из CSV - файла
     * @param table название таблицы в базе данных
     * @param path путь до источника данных (CSV - файла)
     * TODO Добавить Exception для ощибок с чтением файла*/
    fun fillTableFromCSV(table: String, path: String) {

        println("Заполнение таблицы $table из файла $path")
        val s = connection?.createStatement()
        try {
            var requestTemplate = "INSERT INTO `${table}` "
            val dataBufferedReader = File(path).bufferedReader()
            val columns = dataBufferedReader.readLine()
                .split(',')
                .toString()
            requestTemplate += "(${columns.substring(1, columns.length - 1)}) VALUES "

            while (dataBufferedReader.ready()) {
                var request = "$requestTemplate("
                val data = dataBufferedReader.readLine().split(',')
                data.forEachIndexed { i, column ->
                    request += "\"$column\""
                    if (i < data.size - 1) request += ','
                }
                request += ')'
                s?.addBatch(request)
            }
            s?.executeBatch()
            s?.clearBatch()

        } catch (e: SQLException) {
            println(e)
        }

    }

}