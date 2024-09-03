package com.ebicep.chatplus.features.chatwindows

import kotlinx.serialization.Serializable

@Serializable
data class Padding(var left: Int = 0, var right: Int = 0, var bottom: Int = 0) {
    fun clone(): Padding {
        return Padding(left, right, bottom)
    }
}