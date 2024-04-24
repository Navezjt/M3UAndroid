package com.m3u.core.unit

import java.math.BigDecimal
import java.math.RoundingMode

val Double.GB: DataUnit.GB get() = DataUnit.GB(this)
val Double.MB: DataUnit.MB get() = DataUnit.MB(this)
val Double.KB: DataUnit.KB get() = DataUnit.KB(this)

sealed class DataUnit {
    data class GB(val value: Double) : DataUnit() {
        override fun toString(): String = "${value.toInt()} GB"
    }

    data class MB(val value: Double) : DataUnit() {
        override fun toString(): String = "${value.toInt()} MB"
    }

    data class KB(val value: Double) : DataUnit() {
        override fun toString(): String = "${value.toInt()} KB"
    }

    companion object {
        private const val KB: Long = 1024
        private const val MB = KB * 1024
        private const val GB = MB * 1024
        fun of(value: Long): DataUnit {
            val size: Float
            val decimal: BigDecimal

            return if (value == -1L) Unspecified
            else if (value / GB >= 1) {
                size = value / GB.toFloat()
                decimal = BigDecimal(size.toDouble())
                decimal.setScale(2, RoundingMode.HALF_UP).toFloat().toDouble().GB
            } else if (value / MB >= 1) {
                size = value / MB.toFloat()
                decimal = BigDecimal(size.toDouble())
                decimal.setScale(2, RoundingMode.HALF_UP).toFloat().toDouble().MB
            } else if (value / KB >= 1) {
                size = value / KB.toFloat()
                decimal = BigDecimal(size.toDouble())
                decimal.setScale(0, RoundingMode.HALF_UP).toInt().toDouble().KB
            } else {
                0.0.KB
            }
        }
    }

    data object Unspecified : DataUnit()
}
