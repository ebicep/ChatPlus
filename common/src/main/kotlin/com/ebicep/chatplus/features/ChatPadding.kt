package com.ebicep.chatplus.features

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.ChatTabAddDisplayMessageEvent
import com.ebicep.chatplus.features.chattabs.ChatTabGetMessageAtEvent
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.util.GraphicsUtil.fill0
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import com.ebicep.chatplus.util.KotlinUtil.reduceAlpha
import kotlinx.serialization.Serializable
import net.minecraft.client.GuiMessage
import kotlin.math.max
import kotlin.math.roundToInt

object ChatPadding {

    @Serializable
    data class Padding(var left: Int = 0, var right: Int = 0, var bottom: Int = 0) {
        fun clone(): Padding {
            return Padding(left, right, bottom)
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
            it.addMouseOperator { _, current ->
                current.y -= -chatWindow.padding.bottom
            }
            it.addChatOperator { _, current ->
                current.x -= getXTranslation(chatWindow)
            }
        }
        EventBus.register<ChatRenderPreLinesRenderEvent> {
            val chatWindow = it.chatWindow
            val bottomPadding = chatWindow.padding.bottom
            if (bottomPadding == 0) {
                return@register
            }
            val renderer = chatWindow.renderer
            val chatFocused = ChatManager.isChatFocused()
            val chatPlusGuiMessageLine: ChatTab.ChatPlusGuiMessageLine = chatWindow.selectedTab.displayedMessages.lastOrNull() ?: return@register
            val line: GuiMessage.Line = chatPlusGuiMessageLine.line
            val ticksLived: Int = it.guiTicks - line.addedTime()
            if (ticksLived >= 200 && !chatFocused) {
                return@register
            }
            val fadeOpacity = if (chatFocused) 1.0 else renderer.getTimeFactor(ticksLived)
            // bottom padding
            it.guiGraphics.fill0(
                renderer.rescaledX,
                renderer.rescaledY,
                renderer.rescaledEndX,
                renderer.rescaledY - (bottomPadding / chatWindow.scale),
                reduceAlpha(chatWindow.getUpdatedBackgroundColor(), fadeOpacity)
            )
//            // top padding
//            if (chatFocused) {
//                var linesPerPage = renderer.rescaledLinesPerPage
//                it.guiGraphics.fill0(
//                    renderer.rescaledX,
//                    renderer.rescaledY - renderer.getTotalLineHeight(),
//                    renderer.rescaledEndX,
//                    renderer.rescaledY - renderer.getTotalLineHeight() + (renderer.getLinesPerPageScaled() - min(
//                        chatWindow.selectedTab.displayedMessages.size,
//                        linesPerPage
//                    )) * renderer
//                        .lineHeight,
//                    reduceAlpha(chatWindow.getUpdatedBackgroundColor(), fadeOpacity)
//                )
//            }
        }
        EventBus.register<ChatRenderPreLineAppearanceEvent>({ 100 }) {
            val bottomPadding = it.chatWindow.padding.bottom
            if (bottomPadding == 0) {
                return@register
            }
            it.guiGraphics.pose().translate0(y = -bottomPadding / it.chatWindow.scale)
        }
        EventBus.register<ChatRenderLineTextEvent>({ 100 }) {
            val bottomPadding = it.chatWindow.padding.bottom
            if (bottomPadding == 0) {
                return@register
            }
            it.guiGraphics.pose().translate0(y = -bottomPadding / it.chatWindow.scale)
        }
        EventBus.register<GetHeightEvent> {
            if (it.heightType != HeightType.ADJUSTED && it.heightType != HeightType.RENDERED_LINES) {
                return@register
            }
            val chatWindow = it.chatWindow
            val bottomPadding = chatWindow.padding.bottom
            if (bottomPadding == 0) {
                return@register
            }
//            if (it.heightType == HeightType.RENDERED_LINES) {
//                bottomPadding += 20
//            }
            val renderer = chatWindow.renderer
            val lineHeightScaled = renderer.lineHeight * renderer.scale
            val newHeight = it.startingHeight - (bottomPadding * chatWindow.scale).roundToInt()
            it.startingHeight = (newHeight - (newHeight % lineHeightScaled) + lineHeightScaled).toInt()
        }
        EventBus.register<GetTotalLineHeightEvent> {
            it.totalLineHeight += it.chatWindow.padding.bottom
        }
        EventBus.register<GetMaxHeightEvent> {
            if (it.heightType != HeightType.ADJUSTED) {
                return@register
            }
            it.maxHeight -= it.chatWindow.padding.bottom
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