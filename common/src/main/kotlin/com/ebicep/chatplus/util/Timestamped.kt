package com.ebicep.chatplus.util

import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.hud.ChatRenderPreLineAppearanceEvent

abstract class Timestamped(val expiryTick: Long) {

    abstract fun matches(event: ChatRenderPreLineAppearanceEvent): Boolean

}

class TimeStampedLines(val lines: MutableList<ChatTab.ChatPlusGuiMessageLine>, expiryTick: Long) : Timestamped(expiryTick) {

    override fun matches(event: ChatRenderPreLineAppearanceEvent): Boolean {
        return lines.any { it.line == event.line } && expiryTick > Events.currentTick
    }

}

class TimeStampedMessages(val lines: MutableList<ChatTab.ChatPlusGuiMessage>, expiryTick: Long) : Timestamped(expiryTick) {

    override fun matches(event: ChatRenderPreLineAppearanceEvent): Boolean {
        return lines.any { it == event.chatPlusGuiMessageLine.linkedMessage } && expiryTick > Events.currentTick
    }

}