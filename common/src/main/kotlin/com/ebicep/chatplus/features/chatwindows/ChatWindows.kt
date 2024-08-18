package com.ebicep.chatplus.features.chatwindows

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_HEIGHT
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
        EventBus.register<ChatRenderPostLinesEvent> {
            if (!ChatManager.isChatFocused()) {
                return@register
            }
            val chatWindow = it.chatWindow
            if (!chatWindow.outline) {
                return@register
            }
            val renderer = chatWindow.renderer
            val guiGraphics = it.guiGraphics
            val poseStack = guiGraphics.pose()
            poseStack.createPose {
                poseStack.guiForward(amount = 150.0)
                val h = it.displayMessageIndex * renderer.lineHeight
                guiGraphics.renderOutline(renderer.internalX, renderer.internalY - h, renderer.width, h, chatWindow.outlineColor)
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
        val endX = startX + renderer.getUpdatedWidth()
        val startY = renderer.getUpdatedY() - renderer.getUpdatedHeight()
        val endY = renderer.getUpdatedY()
        return startX < x && x < endX && startY < y && y < endY
    }

    private fun selectWindow(chatWindow: ChatWindow) {
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