package com.ebicep.chatplus.features

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatManager

import com.ebicep.chatplus.hud.ChatRenderPreLineAppearanceEvent
import java.awt.Color

object AlternatingColorBackground {

    init {
        // LOOKS LIKE SHIT!!!
        EventBus.register<ChatRenderPreLineAppearanceEvent> {
            val line = it.chatPlusGuiMessageLine
            it.backgroundColor = if (ChatManager.globalSelectedTab.displayedMessages.indexOf(line) % 2 == 0) 1681011250 else 1261580850
        }
    }

}

fun main() {
    println((255 * .5f).toInt() shl 24)
    val rgb = Color(50, 255, 255, 150).rgb
    println(rgb)
    println((rgb shr 24) and 0xFF)
    println((rgb shr 24))
    println(Color(rgb, true).alpha)
    println((0 or 3) shl 16)
    println(Color(0, 0, 0, 25).rgb)
    println(Color(247, 193, 97, 250).rgb)
    println(Color(-1677787036, true))
    println(Color(0 or 3 shl 16).rgb)
    println(Color(0 or 10 shl 16))
    println(Color(0 or 3 shl 16))
    println(Color(-16737281, true))
    println(Color(2130706432, true))
    println(Color((0xFFFFFF55).toInt(), true))
    println(Color(654311423, true))
    //00c800
}