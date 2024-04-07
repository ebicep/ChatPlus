package com.ebicep.chatplus.features

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderLineBackgroundEvent
import java.awt.Color

object AlternatingColorBackground {

    init {
        // LOOKS LIKE SHIT!!!
        EventBus.register<ChatRenderLineBackgroundEvent> {
            val line = it.chatPlusGuiMessageLine
            it.backgroundColor = if (ChatManager.selectedTab.displayedMessages.indexOf(line) % 2 == 0) 1681011250 else 1261580850
        }
    }

}

fun main() {
    println((255 * .5f).toInt() shl 24)
    println(Color(255, 255, 255, 255).rgb)
}