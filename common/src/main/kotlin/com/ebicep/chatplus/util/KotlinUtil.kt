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

}