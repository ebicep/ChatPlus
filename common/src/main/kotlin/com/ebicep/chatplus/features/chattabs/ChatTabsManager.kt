package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.ChatPlusTickEvent
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.MovableChatRemoveTabFromWindowEvent
import com.ebicep.chatplus.features.chatwindows.ChatTabSwitchEvent
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.features.chatwindows.WindowSwitchEvent
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.hud.ChatManager.selectedWindow
import com.ebicep.chatplus.mixin.IMixinChatScreen
import net.minecraft.util.Mth


const val CHAT_TAB_HEIGHT = 15
const val CHAT_TAB_Y_OFFSET = 1 // offset from text box
const val CHAT_TAB_X_SPACE = 1 // space between categories

object ChatTabs {

    val DefaultTab: ChatTab = createDefaultTab()

    fun createDefaultTab(): ChatTab {
        return ChatTab("All", "(?s).*", alwaysAdd = true)
    }

    init {
        EventBus.register<ChatPlusTickEvent> {
            Config.values.chatWindows.forEach { window ->
                window.tabSettings.tabs.forEach { checkTabRefresh(window, it) }
            }
        }
        EventBus.register<ChatRenderPreLinesEvent>({ 100 }) {
            val chatFocused: Boolean = ChatManager.isChatFocused()
            if (chatFocused || it.chatWindow.tabSettings.showTabsWhenChatNotOpen) {
                it.chatWindow.tabSettings.renderTabs(guiGraphics = it.guiGraphics)
            }
        }
        EventBus.register<ChatScreenKeyPressedEvent> {
            if (!Config.values.arrowCycleTabEnabled) {
                return@register
            }
            if (Config.values.movableChatEnabled) {
                return@register
            }
            it.screen as IMixinChatScreen
            if (it.screen.input.value.isNotEmpty()) {
                return@register
            }
            val keyCode = it.keyCode
            if (keyCode == 263) { // left arrow
                selectedWindow.tabSettings.scrollTab(-1)
            } else if (keyCode == 262) { // right arrow
                selectedWindow.tabSettings.scrollTab(1)
            }
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            if (selectedWindow.tabSettings.hideTabs) {
                return@register
            }
            if (it.button == 0) {
                selectedWindow.tabSettings.handleClickedTab(it.mouseX, it.mouseY)
            }
        }
        // tab auto prefix
        EventBus.register<ChatScreenSendMessagePreEvent> {
            if (it.message.startsWith("/") && ChatManager.globalSelectedTab.commandsOverrideAutoPrefix) {
                return@register
            }
            it.message = ChatManager.globalSelectedTab.autoPrefix + it.message
        }
        EventBus.register<ChatTabSwitchEvent> {
            it.newTab.read = true
        }
        EventBus.register<WindowSwitchEvent> {
            it.newWindow.tabSettings.selectedTab.read = true
        }
        EventBus.register<MovableChatRemoveTabFromWindowEvent> {
            if (!it.deleted) {
                it.chatWindow.tabSettings.selectedTab.read = true
            }
        }
    }

    private fun checkTabRefresh(chatWindow: ChatWindow, chatTab: ChatTab) {
        if (chatTab.resetDisplayMessageAtTick == Events.currentTick) {
            chatTab.refreshDisplayMessages()
        }
    }

}


