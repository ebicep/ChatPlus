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
import com.ebicep.chatplus.hud.ChatManager.isChatFocused
import com.ebicep.chatplus.hud.ChatManager.selectedWindow
import com.ebicep.chatplus.mixin.IMixinChatScreen
import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientRawInputEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.util.Mth


const val CHAT_TAB_HEIGHT = 15
const val CHAT_TAB_Y_OFFSET = 1 // offset from text box
const val CHAT_TAB_X_SPACE = 1 // space between categories

object ChatTabs {

    fun createDefaultTab(): ChatTab {
        return ChatTab(ServerChatTabSettings("(?s).*").also { settings ->
            settings.name = "All"
            settings.alwaysAdd = true
        })
    }

    init {
        EventBus.register<ChatPlusTickEvent> {
            Config.values.chatWindows.forEach { window ->
                window.tabSettings.tabs.forEach { checkTabRefresh(window, it) }
            }
        }
        EventBus.register<ChatRenderPreLinesEvent>({ 100 }) {
            val chatFocused: Boolean = ChatManager.isChatFocused()
            if (!it.chatWindow.tabSettings.tabPositionsInitialized) {
                it.chatWindow.tabSettings.tabPositionsInitialized = true
                it.chatWindow.tabSettings.renderTabs(guiGraphics = it.guiGraphics)
            } else if (chatFocused || it.chatWindow.tabSettings.showTabsWhenChatNotOpen) {
                it.chatWindow.tabSettings.renderTabs(guiGraphics = it.guiGraphics)
            }
        }
        EventBus.register<ChatScreenKeyPressedEvent> {
            if (Config.values.movableChatEnabled) {
                return@register
            }
            it.screen as IMixinChatScreen
            if (it.screen.input.value.isNotEmpty()) {
                return@register
            }
            val inputEvent = ChatScreenInputEvent(it)
            if (Config.values.keyCycleTabLeft.isDown(inputEvent)) {
                selectedWindow.tabSettings.scrollTab(-1)
            } else if (Config.values.keyCycleTabRight.isDown(inputEvent)) {
                selectedWindow.tabSettings.scrollTab(1)
            }
        }
        ClientRawInputEvent.KEY_PRESSED.register { _, keyCode, _, action, modifiers ->
            if (isChatFocused() || action != 1) {
                return@register EventResult.pass()
            }
            when {
                Config.values.keyCycleTabLeftChatClosed.isDown(keyCode, modifiers) -> {
                    selectedWindow.tabSettings.scrollTab(-1)
                    EventResult.interruptTrue()
                }

                Config.values.keyCycleTabRightChatClosed.isDown(keyCode, modifiers) -> {
                    selectedWindow.tabSettings.scrollTab(1)
                    EventResult.interruptTrue()
                }

                else -> EventResult.pass()
            }
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            if (selectedWindow.tabSettings.hideTabs) {
                return@register
            }
            val mouseX = it.mouseX
            val mouseY = it.mouseY
            if (it.button == 0) {
                selectedWindow.tabSettings.handleClickedTab(mouseX, mouseY)
            } else if (it.button == 1 && Config.values.tabEditorScreen) {
                val clickedTab = selectedWindow.tabSettings.getClickedTab(mouseX, mouseY) ?: return@register
                if (Config.values.windowEditorScreen && Screen.hasShiftDown()) {
                    Minecraft.getInstance().setScreen(Editor.windowEditor(it.screen, selectedWindow))
                } else {
                    Minecraft.getInstance().setScreen(Editor.tabEditor(it.screen, clickedTab))
                }
            }
        }
        // tab auto prefix
        EventBus.register<ChatScreenSendMessagePreEvent> {
            if (it.message.isEmpty()) {
                return@register
            }
            if (it.message.startsWith("/") && ChatManager.globalSelectedTab.commandsOverrideAutoPrefix) {
                return@register
            }
            it.message = ChatManager.globalSelectedTab.autoPrefix + it.message
        }
        EventBus.register<ChatTabSwitchEvent> {
            it.newTab.unreadCount = 0
        }
        EventBus.register<WindowSwitchEvent> {
            it.newWindow.tabSettings.selectedTab.unreadCount = 0
        }
        EventBus.register<MovableChatRemoveTabFromWindowEvent> {
            if (!it.deleted) {
                it.chatWindow.tabSettings.selectedTab.unreadCount = 0
            }
        }
    }

    private fun checkTabRefresh(chatWindow: ChatWindow, chatTab: ChatTab) {
        if (chatTab.resetDisplayMessageAtTick == Events.currentTick) {
            chatTab.refreshDisplayMessages()
        }
    }

}


