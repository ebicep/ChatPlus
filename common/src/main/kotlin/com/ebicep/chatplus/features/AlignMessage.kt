package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.EnumTranslatableName
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatPositionTranslator
import com.ebicep.chatplus.features.chattabs.ChatTabAddDisplayMessageEvent
import com.ebicep.chatplus.features.chattabs.ChatTabGetMessageAtEvent
import com.ebicep.chatplus.features.chattabs.MessageAtType
import com.ebicep.chatplus.features.internal.Debug.debug
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderLineTextEvent
import com.ebicep.chatplus.hud.ChatRenderer
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import kotlinx.serialization.Serializable
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import kotlin.math.max

object AlignMessage {

    init {
        EventBus.register<ChatRenderLineTextEvent> {
            val alignment = it.chatWindow.messageAlignment
            var xTranslation = alignment.translation(it.chatWindow.renderer, it.chatPlusGuiMessageLine.line.content)
            if (alignment == Alignment.RIGHT && Config.values.scrollbarEnabled) {
                xTranslation -= max(0, Config.values.scrollbarWidth)
            }
            it.guiGraphics.pose().translate0(x = xTranslation)
        }
        EventBus.register<ChatTabAddDisplayMessageEvent>({ -1 }) {
            val chatWindow = it.chatWindow
            if (chatWindow.messageAlignment == Alignment.CENTER) {
                it.maxWidth -= 5
            }
        }
        EventBus.register<ChatTabGetMessageAtEvent>({ -1 }) {
            if (it.messageAtType != MessageAtType.COMPONENT) {
                return@register
            }
            val chatTab = it.chatTab
            val chatWindow = chatTab.chatWindow
            it.addChatOperator { _, current ->
                val messageLine = ChatPositionTranslator.getMessageAtLineRelative(it.chatTab, current.x, current.y) ?: return@addChatOperator
                current.x -= chatWindow.messageAlignment.translation(chatWindow.renderer, messageLine.line.content)
            }
        }
        EventBus.register<ChatRenderLineTextEvent> {
            if (!debug) {
                return@register
            }
            if (!ChatManager.isChatFocused()) {
                return@register
            }
            val renderer = it.chatWindow.renderer
            val guiGraphics = it.guiGraphics
            val pose = guiGraphics.pose()
//            pose.createPose {
//                pose.translate0(z = 5000)
//                guiGraphics.drawString(
//                    Minecraft.getInstance().font,
//                    "${Minecraft.getInstance().font.width(it.chatPlusGuiMessageLine.line.content)} | ${getExtraBoldWidth(it.text)}",
//                    renderer.internalX - 10,
//                    it.verticalChatOffset - renderer.lineHeight,
//                    0x00FF00
//                )
//            }
        }
    }

    @Serializable
    enum class Alignment(val key: String, val translation: (renderer: ChatRenderer, text: FormattedCharSequence) -> Double) : EnumTranslatableName {
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
            { renderer, text -> renderer.rescaledWidth - Minecraft.getInstance().font.width(text).toDouble() - 1 }
        ),

        ;

        val translatable: Component = Component.translatable(key)

        override fun getTranslatableName(): Component {
            return translatable
        }

    }

}