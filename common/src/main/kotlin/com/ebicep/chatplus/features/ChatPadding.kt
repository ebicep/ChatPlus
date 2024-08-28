package com.ebicep.chatplus.features

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatTabAddDisplayMessageEvent
import com.ebicep.chatplus.features.chattabs.ChatTabGetMessageAtEvent
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.hud.ChatRenderLineTextEvent
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import kotlinx.serialization.Serializable
import kotlin.math.max

object ChatPadding {

    @Serializable
    data class Padding(var left: Int = 0, var right: Int = 0) {
        fun clone(): Padding {
            return Padding(left, right)
        }
    }

    init {
        EventBus.register<ChatRenderLineTextEvent>({ 10 }) {
            val chatWindow = it.chatWindow
            val guiGraphics = it.guiGraphics
            val poseStack = guiGraphics.pose()
            poseStack.translate0(x = getXTranslation(chatWindow))
        }
        EventBus.register<ChatTabAddDisplayMessageEvent> {
            val chatWindow = it.chatWindow
            val padding = chatWindow.padding
            it.maxWidth -= max(0, padding.left + padding.right)
        }
        EventBus.register<ChatTabGetMessageAtEvent> {
            val chatWindow = it.chatTab.chatWindow
            it.chatX -= getXTranslation(chatWindow)
        }
    }


    private fun getXTranslation(chatWindow: ChatWindow): Int {
        val padding = chatWindow.padding
        return when (chatWindow.messageAlignment) {
            AlignMessage.Alignment.LEFT -> padding.left
            AlignMessage.Alignment.CENTER -> padding.left
            AlignMessage.Alignment.RIGHT -> -padding.right
        }
    }

}