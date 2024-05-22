package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatTabGetMessageAtEvent
import com.ebicep.chatplus.hud.ChatRenderLineTextEvent
import com.ebicep.chatplus.hud.ChatRenderer
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import kotlinx.serialization.Serializable
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object AlignMessage {

    init {
        EventBus.register<ChatRenderLineTextEvent> {
            it.guiGraphics.pose().translate0(x = Config.values.messageAlignment.translation(it.text))
        }
        EventBus.register<ChatTabGetMessageAtEvent> {
            val messageLine = it.chatTab.getMessageAtLineRelative(it.chatX, it.chatY) ?: return@register
            it.chatX -= Config.values.messageAlignment.translation(messageLine.content)
        }
    }

    @Serializable
    enum class Alignment(key: String, val translation: (text: String) -> Double) {
        LEFT(
            "chatPlus.chatSettings.messageAlignment.left",
            { 0.0 }
        ),
        CENTER(
            "chatPlus.chatSettings.messageAlignment.center",
            { ChatRenderer.rescaledWidth / 2.0 - Minecraft.getInstance().font.width(it) / 2.0 }
        ),
        RIGHT(
            "chatPlus.chatSettings.messageAlignment.right",
            { (ChatRenderer.rescaledWidth - Minecraft.getInstance().font.width(it)).toDouble() }
        ),

        ;

        val translatable: Component = Component.translatable(key)

    }

}