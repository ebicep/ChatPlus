package com.ebicep.chatplus.features.chatwindows

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_HEIGHT
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_Y_OFFSET
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
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
        EventBus.register<ChatRenderPreLinesEvent> {
            if (!ChatManager.isChatFocused()) {
                return@register
            }
            val chatWindow = it.chatWindow
            if (!chatWindow.outline.enabled) {
                return@register
            }
            val selectedTab = chatWindow.selectedTab
            val renderer = chatWindow.renderer
            val guiGraphics = it.guiGraphics
            val poseStack = guiGraphics.pose()
            poseStack.createPose {
                poseStack.guiForward(if (ChatManager.globalSelectedTab == selectedTab) 700.0 else 500.0)
                val outlineBoxType = chatWindow.outline.outlineBoxType
                val outlineTabType = chatWindow.outline.outlineTabType
                outlineBoxType.render(outlineTabType, guiGraphics, chatWindow, selectedTab, renderer)
                if (!chatWindow.hideTabs) {
                    outlineTabType.render(outlineBoxType, guiGraphics, chatWindow, selectedTab, renderer)
                }
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
//
//    private fun render(
//        renderer: ChatRenderer,
//        messagesToDisplay: Int,
//        guiGraphics: GuiGraphics,
//        chatWindow: ChatWindow,
//        selectedTab: ChatTab
//    ) {
//        val lineCount = if (Config.values.movableChatEnabled) renderer.rescaledLinesPerPage else min(messagesToDisplay, renderer.rescaledLinesPerPage)
//        val h = lineCount * (renderer.lineHeight * renderer.scale)
//        val w = renderer.internalWidth
//        guiGraphics.renderOutline(
//            renderer.internalX.toFloat() - THICKNESS,
//            renderer.internalY.toFloat() - h - THICKNESS,
//            w.toFloat() + THICKNESS + THICKNESS,
//            h + THICKNESS + THICKNESS + THICKNESS,
//            chatWindow.outlineColor,
//            THICKNESS.toFloat(),
//            bottom = false
//        )
//        val tabStartX = selectedTab.xStart - THICKNESS
//        val tabEndX = selectedTab.xEnd + THICKNESS
//        // tab U shaped box
//        guiGraphics.renderOutline(
//            tabStartX,
//            selectedTab.yStart - CHAT_TAB_Y_OFFSET * 2,
//            tabEndX - tabStartX,
//            CHAT_TAB_HEIGHT,
//            chatWindow.outlineColor,
//            THICKNESS,
//            top = false
//        )
//        // tab sides for dragging tab down
//        guiGraphics.drawVerticalLine(
//            tabStartX,
//            renderer.internalY,
//            selectedTab.yStart,
//            chatWindow.outlineColor,
//            THICKNESS
//        )
//        guiGraphics.drawVerticalLine(
//            selectedTab.xEnd,
//            renderer.internalY,
//            selectedTab.yStart,
//            chatWindow.outlineColor,
//            THICKNESS
//        )
//        // chat bottom left/right
//        guiGraphics.drawHorizontalLine(
//            renderer.internalX,
//            tabStartX,
//            renderer.internalY,
//            chatWindow.outlineColor,
//            THICKNESS
//        )
//        guiGraphics.drawHorizontalLine(
//            tabEndX,
//            renderer.backgroundWidthEndX,
//            renderer.internalY,
//            chatWindow.outlineColor,
//            THICKNESS
//        )
//    }

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