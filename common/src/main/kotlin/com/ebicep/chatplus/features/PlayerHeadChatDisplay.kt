package com.ebicep.chatplus.features

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatRenderLineTextEvent

object PlayerHeadChatDisplay {

    init {
//            Minecraft.getInstance().connection.onlinePlayers.forEach {
//                it.skin.texture
//            }
        EventBus.register<ChatRenderLineTextEvent> {
//            it.guiGraphics.blit()

        }
    }

}