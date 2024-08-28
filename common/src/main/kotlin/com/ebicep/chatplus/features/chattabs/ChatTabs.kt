package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.ChatPlusTickEvent
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.features.chatwindows.ChatWindows.DefaultWindow
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.hud.ChatManager.selectedWindow
import com.ebicep.chatplus.mixin.IMixinChatScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth


const val CHAT_TAB_HEIGHT = 15
const val CHAT_TAB_Y_OFFSET = 1 // offset from text box
const val CHAT_TAB_X_SPACE = 1 // space between categories

data class ChatTabClickedEvent(
    val chatTab: ChatTab,
    val mouseX: Double,
    val mouseY: Double,
    val tabXStart: Double,
    val tabYStart: Double
)

data class ChatTabRenderEvent(
    val guiGraphics: GuiGraphics,
    val chatTab: ChatTab,
    val tabWidth: Int,
    var xStart: Int,
    var yStart: Int
)

data class ChatTabSwitchEvent(
    val oldTab: ChatTab,
    val newTab: ChatTab
)

object ChatTabs {

    val DefaultTab: ChatTab = ChatTab("All", "(?s).*", alwaysAdd = true)

    init {
        EventBus.register<ChatPlusTickEvent> {
            if (!Config.values.chatWindowsTabsEnabled) {
                checkTabRefresh(DefaultWindow, DefaultTab)
            } else {
                Config.values.chatWindows.forEach { window -> window.tabs.forEach { checkTabRefresh(window, it) } }
            }
        }
        EventBus.register<ChatRenderPreLinesEvent>({ 100 }) {
            if (!Config.values.chatWindowsTabsEnabled) {
                return@register
            }
            val chatFocused: Boolean = ChatManager.isChatFocused()
            if (chatFocused) {
                it.chatWindow.renderTabs(guiGraphics = it.guiGraphics)
            }
        }
        EventBus.register<ChatScreenKeyPressedEvent> {
            if (!Config.values.chatWindowsTabsEnabled || !Config.values.arrowCycleTabEnabled) {
                return@register
            }
            it.screen as IMixinChatScreen
            if (it.screen.input.value.isNotEmpty()) {
                return@register
            }
            val keyCode = it.keyCode
            if (keyCode == 263) { // left arrow
                selectedWindow.scrollTab(-1)
            } else if (keyCode == 262) { // right arrow
                selectedWindow.scrollTab(1)
            }
        }
        EventBus.register<ChatScreenMouseScrolledEvent> {
            if (!Config.values.chatWindowsTabsEnabled || !Config.values.scrollCycleTabEnabled) {
                return@register
            }
            val amountX = it.amountX
            if (amountX == 0.0) {
                return@register
            }
            selectedWindow.scrollTab(Mth.clamp(-amountX.toInt(), -1, 1))
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            if (!Config.values.chatWindowsTabsEnabled) {
                return@register
            }
            if (selectedWindow.hideTabs) {
                return@register
            }
            if (it.button == 0) {
                selectedWindow.handleClickedTab(it.mouseX, it.mouseY)
            }
        }
        // tab auto prefix
        EventBus.register<ChatScreenSendMessagePreEvent> {
            it.message = ChatManager.globalSelectedTab.autoPrefix + it.message
        }
    }

    private fun checkTabRefresh(chatWindow: ChatWindow, chatTab: ChatTab) {
        if (chatTab.resetDisplayMessageAtTick == Events.currentTick) {
            chatTab.refreshDisplayMessages()
        }
    }

}


