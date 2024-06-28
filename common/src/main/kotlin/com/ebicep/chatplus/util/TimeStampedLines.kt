package com.ebicep.chatplus.util

import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chattabs.ChatTab
import net.minecraft.client.GuiMessage.Line

class TimeStampedLines(val lines: MutableList<ChatTab.ChatPlusGuiMessageLine>, private val expiryTick: Long) {

    constructor(line: ChatTab.ChatPlusGuiMessageLine, expiryTick: Long) : this(mutableListOf(line), expiryTick)

    fun matches(otherLine: Line): Boolean {
        return lines.any { it.line == otherLine } && expiryTick > Events.currentTick
    }

}