package com.ebicep.chatplus.features.internal


import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.ChatFormatting

@Serializable
open class MessageFilter {

    var pattern: String = ""
        set(value) {
            field = value
            regex = Regex(value)
        }

    // if regex should match messages with formatting
    var formatted: Boolean = false

    constructor(pattern: String) {
        this.pattern = pattern
        this.regex = Regex(pattern)
    }

    @Transient
    var regex: Regex = Regex("")

    fun matches(message: String): Boolean {
        return if (formatted) {
            regex.matches(message.replace("§", "&"))
        } else {
            regex.matches(ChatFormatting.stripFormatting(message)!!)
        }
    }

}