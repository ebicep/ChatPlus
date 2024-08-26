package com.ebicep.chatplus.features.chatwindows

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_HEIGHT
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_Y_OFFSET
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.drawHorizontalLine
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.GraphicsUtil.renderOutline
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import net.minecraft.client.gui.GuiGraphics

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
        EventBus.register<ChatRenderPostLinesEvent> {
            if (!ChatManager.isChatFocused()) {
                return@register
            }
            val chatWindow = it.chatWindow
            if (!chatWindow.outline) {
                return@register
            }
            val selectedTab = chatWindow.selectedTab
            val renderer = chatWindow.renderer
            val guiGraphics = it.guiGraphics
            val poseStack = guiGraphics.pose()
            poseStack.createPose {
                poseStack.guiForward(amount = 150.0)
                val thickness = 1 / renderer.scale
                val h = (if (Config.values.movableChatEnabled) renderer.rescaledHeight.toFloat() else it.displayMessageIndex * renderer.lineHeight.toFloat()) + thickness
                val w = renderer.rescaledEndX - renderer.rescaledX
                guiGraphics.renderOutline(
                    renderer.rescaledX - thickness,
                    renderer.rescaledY - h,
                    w + thickness + thickness,
                    h + thickness + thickness,
                    chatWindow.outlineColor,
                    thickness,
                    bottom = false
                )
                val tabStartX = selectedTab.xStart / renderer.scale - thickness
                val tabEndX = selectedTab.xEnd / renderer.scale + thickness
                guiGraphics.renderOutline(
                    tabStartX,
                    renderer.rescaledY - CHAT_TAB_Y_OFFSET,
                    tabEndX - tabStartX,
                    CHAT_TAB_HEIGHT / renderer.scale,
                    chatWindow.outlineColor,
                    thickness,
                    top = false
                )
                guiGraphics.drawHorizontalLine(
                    renderer.rescaledX,
                    tabStartX,
                    renderer.rescaledY,
                    chatWindow.outlineColor,
                    thickness
                )
                guiGraphics.drawHorizontalLine(
                    tabEndX,
                    renderer.rescaledEndX,
                    renderer.rescaledY,
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