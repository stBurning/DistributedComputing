@file:Suppress("unused")

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.*


open class DoubleMatrix(
    @JsonProperty("cols") open val cols: Int,
    @JsonProperty("rows") open val rows: Int,
    @JsonProperty("list") val list: MutableList<Double>
) {


    operator fun set(x: Int, y: Int, value: Double) {
        list[x * cols + y] = value
    }

    open operator fun get(x: Int, y: Int): Double {
        return list[x * cols + y]
    }

    @JsonProperty("list")
    fun getMutableList(): List<Double?> {
        return list
    }

    fun toJson(): String {
        val mapper = ObjectMapper()
        return mapper.writeValueAsString(this)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append('[')
        forEachIndexed { x, y, value ->
            if (y == 0)
                sb.append('[')
            sb.append(value.toString())
            if (y == cols - 1) {
                sb.append(']')
                if (x < rows - 1)
                    sb.append(",\n")
            } else {
                sb.append(", ")
            }
        }
        sb.append(']')
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is DoubleMatrix) return false
        if (rows != other.rows || cols != other.cols) return false

        var eq = true
        forEachIndexed { x, y, value ->
            if (value != other[x, y]) {
                eq = false
                return@forEachIndexed
            }
        }
        return eq
    }

    override fun hashCode(): Int {
        var h = 17
        h = h * 39 + cols
        h = h * 39 + rows
        forEach { h = h * 37 + it.hashCode() }
        return h
    }
}

fun doubleMatrixFromJson(json: String): DoubleMatrix? {
    val mapper = ObjectMapper()
    return mapper.readValue(json, DoubleMatrix::class.java)
}

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
inline fun createDoubleMatrix(cols: Int, rows: Int, init: (Int, Int) -> Double): DoubleMatrix {
    return DoubleMatrix(cols, rows, prepareListForMatrix(cols, rows, init))
}


infix fun DoubleMatrix.x(other: DoubleMatrix): DoubleMatrix {
    if (cols != other.rows)
        throw IllegalArgumentException("Matrices not match")

    return createDoubleMatrix(rows, other.cols) { x, y ->
        var value = .0
        for (i in 0 until cols)
            value += this[x, i] * other[i, y]
        value
    }
}


internal open class TransposedMatrix(protected val original: DoubleMatrix) :
    DoubleMatrix(original.cols, original.rows, original.list) {
    override val cols: Int
        get() = original.rows

    override val rows: Int
        get() = original.cols

    override fun get(x: Int, y: Int): Double = original[y, x]
}

fun DoubleMatrix.asTransposed(): DoubleMatrix = TransposedMatrix(this)


private inline fun <T> prepareListForMatrix(cols: Int, rows: Int, init: (Int, Int) -> T): ArrayList<T> {
    val list = ArrayList<T>(cols * rows)
    for (y in 0 until rows) {
        for (x in 0 until cols) {
            list.add(init(x, y))
        }
    }
    return list
}


inline fun DoubleMatrix.mapIndexed(transform: (Int, Int, Double) -> Double): DoubleMatrix {
    return createDoubleMatrix(cols, rows) { x, y -> transform(x, y, this[x, y]) }
}

inline fun DoubleMatrix.map(transform: (Double) -> Double): DoubleMatrix {
    return mapIndexed { x, y, value -> transform(value) }
}

inline fun DoubleMatrix.forEachIndexed(action: (Int, Int, Double) -> Unit) {
    for (x in 0 until rows) {
        for (y in 0 until cols) {
            action(x, y, this[x, y])
        }
    }
}

inline fun DoubleMatrix.forEach(action: (Double) -> Unit) {
    forEachIndexed { _, _, value -> action(value) }
}

fun DoubleMatrix.toList(): List<Double> {
    return prepareListForMatrix(cols, rows) { x, y -> this[x, y] }
}

fun DoubleMatrix.toMutableList(): MutableList<Double> {
    return prepareListForMatrix(cols, rows) { x, y -> this[x, y] }
}

