package com.ebicep.chatplus.features.chatwindows

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_HEIGHT
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_Y_OFFSET
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.drawHorizontalLine
import com.ebicep.chatplus.util.GraphicsUtil.drawVerticalLine
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.GraphicsUtil.renderOutline
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.min

object ChatWindows {

    val DefaultWindow = ChatWindow()

    init {
        EventBus.register<ChatScreenMouseClickedEvent>({ 10000 }) {
            // check if mouse in inside widows starting from last
            for (i in Config.values.chatWindows.size - 1 downTo 0) {
                if (insideWindow(Config.values.chatWindows[i], it.mouseX, it.mouseY)) {
                    selectWindow(Config.values.chatWindows[i])
                    return@register
                }
            }
        }
        EventBus.register<ChatRenderPreLinesEvent> {
            if (!ChatManager.isChatFocused()) {
                return@register
            }
            val chatWindow = it.chatWindow
            if (!chatWindow.outline) {
                return@register
            }
            val selectedTab = chatWindow.selectedTab
            val messagesToDisplay = selectedTab.displayedMessages.size
            val renderer = chatWindow.renderer
            val guiGraphics = it.guiGraphics
            val poseStack = guiGraphics.pose()
            poseStack.createPose {
                poseStack.guiForward(amount = 150.0)
                val thickness = 1
                val lineCount = if (Config.values.movableChatEnabled) renderer.rescaledLinesPerPage else min(messagesToDisplay, renderer.rescaledLinesPerPage)
                val h = lineCount * (renderer.lineHeight * renderer.scale)
                val w = renderer.internalWidth
                guiGraphics.renderOutline(
                    renderer.internalX.toFloat() - thickness,
                    renderer.internalY.toFloat() - h - thickness,
                    w.toFloat() + thickness + thickness,
                    h + thickness + thickness + thickness,
                    chatWindow.outlineColor,
                    thickness.toFloat(),
                    bottom = false
                )
                val tabStartX = selectedTab.xStart - thickness
                val tabEndX = selectedTab.xEnd + thickness
                // tab U shaped box
                guiGraphics.renderOutline(
                    tabStartX,
                    selectedTab.yStart - CHAT_TAB_Y_OFFSET * 2,
                    tabEndX - tabStartX,
                    CHAT_TAB_HEIGHT,
                    chatWindow.outlineColor,
                    thickness,
                    top = false
                )
                // tab sides for dragging tab down
                guiGraphics.drawVerticalLine(
                    tabStartX,
                    renderer.internalY,
                    selectedTab.yStart,
                    chatWindow.outlineColor,
                    thickness
                )
                guiGraphics.drawVerticalLine(
                    selectedTab.xEnd,
                    renderer.internalY,
                    selectedTab.yStart,
                    chatWindow.outlineColor,
                    thickness
                )
                // chat bottom left/right
                guiGraphics.drawHorizontalLine(
                    renderer.internalX,
                    tabStartX,
                    renderer.internalY,
                    chatWindow.outlineColor,
                    thickness
                )
                guiGraphics.drawHorizontalLine(
                    tabEndX,
                    renderer.backgroundWidthEndX,
                    renderer.internalY,
                    chatWindow.outlineColor,
                    thickness
                )
            }
        }
        EventBus.register<GetMaxHeightEvent> {
            if (it.chatWindow.hideTabs) {
                return@register
            }
            it.maxHeight -= CHAT_TAB_HEIGHT
        }
        EventBus.register<GetDefaultYEvent> {
            if (it.chatWindow.hideTabs) {
                return@register
            }
            it.y -= CHAT_TAB_HEIGHT
        }
    }

    private fun insideWindow(chatWindow: ChatWindow, x: Double, y: Double): Boolean {
        val renderer = chatWindow.renderer
        val startX = renderer.getUpdatedX()
        val endX = startX + renderer.getUpdatedWidthValue()
        val startY = renderer.getUpdatedY() - renderer.getUpdatedHeight()
        var endY = renderer.getUpdatedY()
        if (!chatWindow.hideTabs) {
            endY += CHAT_TAB_HEIGHT + CHAT_TAB_Y_OFFSET
        }
        return startX < x && x < endX && startY < y && y < endY
    }

    fun selectWindow(chatWindow: ChatWindow) {
        Config.values.chatWindows.remove(chatWindow)
        Config.values.chatWindows.add(chatWindow)
    }

    fun renderAll(guiGraphics: GuiGraphics, i: Int, j: Int, k: Int) {
        Config.values.chatWindows.forEachIndexed { index, it ->
            val poseStack = guiGraphics.pose()
            poseStack.createPose {
                poseStack.translate0(z = index * 1000)
                it.renderer.render(it, guiGraphics, i, j, k)
            }
        }
    }

}