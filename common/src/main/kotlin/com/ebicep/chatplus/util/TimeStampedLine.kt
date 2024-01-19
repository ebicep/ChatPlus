package com.ebicep.chatplus.util

import com.ebicep.chatplus.events.Events
import net.minecraft.client.GuiMessage.Line

class TimeStampedLine(val line: Line, val expiryTick: Long) {

    fun matches(otherLine: Line): Boolean {
        return line == otherLine && expiryTick > Events.currentTick
    }

}