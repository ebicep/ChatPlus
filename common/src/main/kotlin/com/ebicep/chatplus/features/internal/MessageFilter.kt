package com.ebicep.chatplus.features.internal


import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
open class MessageFilter {

    var pattern: String = ""
        set(value) {
            field = value
            regex = Regex(value)
        }

    constructor(pattern: String) {
        this.pattern = pattern
        this.regex = Regex(pattern)
    }

    @Transient
    var regex: Regex = Regex("")

}