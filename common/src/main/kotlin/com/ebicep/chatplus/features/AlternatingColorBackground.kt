package com.ebicep.chatplus.features

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderLineBackgroundEvent

object AlternatingColorBackground {

    init {
        // LOOKS LIKE SHIT!!!
        EventBus.register<ChatRenderLineBackgroundEvent> {
            val line = it.chatPlusGuiMessageLine
            it.backgroundColor = if (ChatManager.selectedTab.displayedMessages.indexOf(line) % 2 == 0) 1681011250 else 1261580850
        }
    }

}

//fun main() {
////    println(Color(0, 155, 255, 255).rgb)
//    val listOf = mutableListOf(1, 2, 3, 4, 5)
//    listOf.add(2, 5)
//    println(listOf)
//}