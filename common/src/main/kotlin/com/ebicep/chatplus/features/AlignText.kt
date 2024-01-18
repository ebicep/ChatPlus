package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatRenderLineTextEvent
import com.ebicep.chatplus.hud.ChatRenderer
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import kotlinx.serialization.Serializable
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object AlignText {

    init {
        EventBus.register<ChatRenderLineTextEvent> {
            it.guiGraphics.pose().translate0(x = Config.values.textAlignment.translation(it.text))
        }
    }

    @Serializable
    enum class Alignment(private val key: String, val translation: (text: String) -> Double) {
        LEFT("chatPlus.chatSettings.textAlignment.left", {
            0.0
        }),
        CENTER("chatPlus.chatSettings.textAlignment.center", {
            ChatRenderer.rescaledWidth / 2.0 - Minecraft.getInstance().font.width(it) / 2.0
        }),
        RIGHT("chatPlus.chatSettings.textAlignment.right", {
            (ChatRenderer.rescaledWidth - Minecraft.getInstance().font.width(it)).toDouble()
        }),

        ;

        val translatable: Component = Component.translatable(key)

    }

}