package com.ebicep.chatplus.features.internal


import com.ebicep.chatplus.features.chattabs.ChatTab.ChatPlusGuiMessageLine
import com.ebicep.chatplus.util.ComponentUtil.getColoredString
import kotlinx.serialization.Serializable
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

@Serializable
open class MessageFilterFormatted : MessageFilter {

    // if regex should match messages with formatting
    var formatted: Boolean = false

    constructor(pattern: String, formatted: Boolean = false) : super(pattern) {
        this.formatted = formatted
    }

    fun matches(message: String, coloredMessage: String?): Boolean {
        if (pattern == "(?s).*" || pattern == "(.*?)") {
            return true
        }
        return if (formatted) {
            regex.matches(coloredMessage ?: message)
        } else {
            regex.matches(ChatFormatting.stripFormatting(message)!!)
        }
    }

    fun matches(message: Component): Boolean {
        return matches(message.string, message.visualOrderText.getColoredString())
    }

    fun matches(message: ChatPlusGuiMessageLine): Boolean {
        return matches(message.content, message.coloredContent())
    }

    fun find(message: String): MatchResult? {
        return if (formatted) {
            regex.find(message.replace("§", "&"))
        } else {
            regex.find(ChatFormatting.stripFormatting(message)!!)
        }
    }

}