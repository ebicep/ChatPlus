package com.ebicep.chatplus.features.internal


import kotlinx.serialization.Serializable
import net.minecraft.ChatFormatting

@Serializable
open class MessageFilterFormatted : MessageFilter {

    // if regex should match messages with formatting
    var formatted: Boolean = false

    constructor(pattern: String, formatted: Boolean = false) : super(pattern) {
        this.formatted = formatted
    }

    open fun matches(message: String, pattern: String, regex: Regex): Boolean {
        if (pattern == "(?s).*" || pattern == "(.*?)") {
            return true
        }
        return if (formatted) {
            regex.matches(message.replace("§", "&"))
        } else {
            regex.matches(ChatFormatting.stripFormatting(message)!!)
        }
    }

    fun matches(message: String): Boolean {
        return matches(message, pattern, regex)
    }

    fun find(message: String): MatchResult? {
        return if (formatted) {
            regex.find(message.replace("§", "&"))
        } else {
            regex.find(ChatFormatting.stripFormatting(message)!!)
        }
    }

}