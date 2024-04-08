package com.ebicep.chatplus.features

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatRenderLineTextEvent

object PlayerHeadChatDisplay {

//    private const val NAME_REGEX = Pattern("(§.)|\\W")

    init {
//        EventBus.register<ChatTabAddNewMessageEvent> {
//            val content = it.component.string
//            content.split(NAME_REGEX).forEach { word ->
//                if (word.isNullOrBlank()) {
//                    return@forEach
//                }
//            }
//        }
//            Minecraft.getInstance().connection.onlinePlayers.forEach {
//                it.skin.texture
//            }
        EventBus.register<ChatRenderLineTextEvent> {
//            it.guiGraphics.blit()

        }
    }

}