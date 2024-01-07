package com.ebicep.chatplus.features

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderLineBackgroundEvent
import java.awt.Color

object AlternatingColorBackground {

    init {
        EventBus.register<ChatRenderLineBackgroundEvent> {
            val line = it.chatPlusGuiMessageLine
            it.backgroundColor = if (ChatManager.selectedTab.displayedMessages.indexOf(line) % 2 == 0) 1681011250 else 1261580850
        }
    }

}

fun main() {
    println(Color(100, 50, 50, 75).rgb)
}