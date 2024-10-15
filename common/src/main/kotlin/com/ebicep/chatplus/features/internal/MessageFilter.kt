package com.ebicep.chatplus.features.internal


import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
open class MessageFilter {

    var pattern: String = ""
        set(value) {
            field = value
            updateRegex()
        }

    constructor(pattern: String) {
        this.pattern = pattern
        updateRegex()
    }

    fun updateRegex() {
        this.regex = Regex(pattern)
    }

    @Transient
    var regex: Regex = Regex("")

}