package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.MovableChat.InputBoxSettings.Companion.INPUT_BOX_PADDING
import com.ebicep.chatplus.features.MovableChat.InputBoxSettings.Companion.PADDED_INPUT_BOX_HEIGHT
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_HEIGHT
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.features.chatwindows.TabSettings
import com.ebicep.chatplus.hud.ChatPlusScreen.EDIT_BOX_HEIGHT
import com.ebicep.chatplus.hud.ChatScreenCloseEvent
import com.ebicep.chatplus.hud.ChatScreenInitPostEvent
import com.ebicep.chatplus.hud.RenderValidateYEvent
import net.minecraft.client.Minecraft
import kotlin.math.roundToInt

object InputBoxAutoAdjustChatWindow {

    init {
        var adjusted = mutableMapOf<ChatWindow, WindowState>()
        EventBus.register<ChatScreenInitPostEvent> {
            if (!Config.values.inputBoxAutoAdjustChatWindowEnabled) {
                return@register
            }
            val inputBoxStartY = Config.values.inputBoxSettings.getCalculatedStartY()
            val inputBoxMaxBottomY = Minecraft.getInstance().window.guiScaledHeight - PADDED_INPUT_BOX_HEIGHT
            val topBar = inputBoxStartY == INPUT_BOX_PADDING
            val bottomBar = inputBoxStartY == inputBoxMaxBottomY
            if (!topBar && !bottomBar) {
                return@register
            }
            Config.values.chatWindows.forEach {
                val renderer = it.renderer
                var internalY = renderer.internalY
                if (bottomBar) {
                    if (it.tabSettings.position == TabSettings.Position.BOTTOM) {
                        internalY += CHAT_TAB_HEIGHT
                    }
                    if (internalY > inputBoxStartY) {
                        val oldY = renderer.internalY
                        renderer.internalY -= internalY - inputBoxStartY + INPUT_BOX_PADDING
                        renderer.rescaledY = renderer.internalY / renderer.scale
                        adjusted[it] = WindowState(oldY, renderer.internalY)
                    }
                } else {
                    if (it.tabSettings.position == TabSettings.Position.TOP) {
                        internalY -= CHAT_TAB_HEIGHT
                    }
                    val topY = internalY - renderer.getTotalLineHeight(true).roundToInt()
                    if (topY < EDIT_BOX_HEIGHT) {
                        val oldY = renderer.internalY
                        renderer.internalY += EDIT_BOX_HEIGHT - topY
                        renderer.rescaledY = renderer.internalY / renderer.scale
                        adjusted[it] = WindowState(oldY, renderer.internalY)
                    }
                }
            }
        }
        EventBus.register<RenderValidateYEvent> {
            adjusted.forEach { (chatWindow, amount) ->
                if (it.renderer == chatWindow.renderer) {
                    it.internalY = amount.oldY
                }
            }
        }
        EventBus.register<ChatScreenCloseEvent> {
            adjusted.forEach { (chatWindow, amount) ->
                if (chatWindow.renderer.internalY == amount.newY) {
                    chatWindow.renderer.internalY = amount.oldY
                    chatWindow.renderer.rescaledY = amount.oldY / chatWindow.renderer.scale
                }
            }
            adjusted.clear()
        }
    }

    data class WindowState(val oldY: Int, val newY: Int)

}
