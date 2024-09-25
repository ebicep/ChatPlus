package com.ebicep.chatplus.util

import java.awt.Color
import kotlin.math.min


object KotlinUtil {

    fun <E> Collection<E>.containsReference(element: @UnsafeVariance E): Boolean {
        for (e in this) {
            if (e === element) {
                return true
            }
        }
        return false
    }

    fun <T> areListsEqual(
        list1: List<T>,
        list2: List<T>,
        comparator: (T, T) -> Boolean
    ): Boolean {
        // Check if the lists are of the same size
        if (list1.size != list2.size) {
            return false
        }

        // Compare each element using the custom comparator
        for (i in list1.indices) {
            if (!comparator(list1[i], list2[i])) {
                return false
            }
        }
        return true
    }

    private const val FACTOR: Double = 0.9

    fun Color.brighter2(): Color {
        var r: Int = getRed()
        var g: Int = getGreen()
        var b: Int = getBlue()
        val alpha: Int = getAlpha()

        val i = (1.0 / (1.0 - FACTOR)).toInt()
        if (r == 0 && g == 0 && b == 0) {
            return Color(i, i, i, alpha)
        }
        if (r in 1..<i) r = i
        if (g in 1..<i) g = i
        if (b in 1..<i) b = i

        return Color(
            min((r / FACTOR).toInt(), 255),
            min((g / FACTOR).toInt(), 255),
            min((b / FACTOR).toInt(), 255),
            alpha
        )
    }

    fun Color.reduceAlpha(percentage: Float): Color {
        val oldAlpha = (rgb shr 24) and 0xff
        val newAlpha = oldAlpha * percentage
        return Color(rgb and 0x00ffffff or (newAlpha.toInt() shl 24), true)
    }

    fun reduceAlpha(color: Int, percentage: Double): Int {
        val oldAlpha = (color shr 24) and 0xff
        val newAlpha = oldAlpha * percentage
        return color and 0x00ffffff or (newAlpha.toInt() shl 24)
    }

    fun reduceAlpha(color: Int, percentage: Float): Int {
        val oldAlpha = (color shr 24) and 0xff
        val newAlpha = oldAlpha * percentage
        return color and 0x00ffffff or (newAlpha.toInt() shl 24)
    }

}