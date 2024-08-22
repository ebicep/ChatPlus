package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.EnumTranslatableName
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatTabGetMessageAtEvent
import com.ebicep.chatplus.hud.ChatRenderLineTextEvent
import com.ebicep.chatplus.hud.ChatRenderer
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import kotlinx.serialization.Serializable
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import kotlin.math.max

object AlignMessage {

    init {
        EventBus.register<ChatRenderLineTextEvent> {
            val alignment = it.chatWindow.messageAlignment
            var xTranslation = alignment.translation(it.chatWindow.renderer, it.text)
            if (alignment == Alignment.RIGHT && Config.values.scrollbarEnabled) {
                xTranslation -= max(0, Config.values.scrollbarWidth)
            }
            it.guiGraphics.pose().translate0(x = xTranslation)
        }
        EventBus.register<ChatTabGetMessageAtEvent> {
            val chatTab = it.chatTab
            val chatWindow = chatTab.chatWindow
            val messageLine = chatTab.getMessageAtLineRelative(it.chatX, it.chatY) ?: return@register
            it.chatX -= chatWindow.messageAlignment.translation(chatWindow.renderer, messageLine.content)
        }
    }

    @Serializable
    enum class Alignment(val key: String, val translation: (renderer: ChatRenderer, text: String) -> Double) : EnumTranslatableName {
        LEFT(
            "chatPlus.chatWindow.messageAlignment.left",
            { _, _ -> 0.0 }
        ),
        CENTER(
            "chatPlus.chatWindow.messageAlignment.center",
            { renderer, text -> renderer.rescaledWidth / 2.0 - Minecraft.getInstance().font.width(text) / 2.0 }
        ),
        RIGHT(
            "chatPlus.chatWindow.messageAlignment.right",
            { renderer, text -> (renderer.rescaledWidth - Minecraft.getInstance().font.width(text)).toDouble() }
        ),

        ;

        val translatable: Component = Component.translatable(key)

        override fun getTranslatableName(): Component {
            return translatable
        }

    }

}