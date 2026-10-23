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
import com.ebicep.chatplus.platform.events.EventResult
import com.ebicep.chatplus.platform.events.client.ClientRawInputEvent
import com.ebicep.chatplus.util.KeyUtil.isLeftMouseButton
import com.ebicep.chatplus.util.KeyUtil.isRightMouseButton
import net.minecraft.client.Minecraft
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
        EventBus.register<ChatRenderPostLinesEvent> {
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
        ClientRawInputEvent.KEY_PRESSED.register { _, keyCode, keyEvent ->
            if (isChatFocused() || keyCode != 1) {
                return@register EventResult.pass()
            }
            val modifiers = keyEvent.modifiers()
            when {
                Config.values.keyCycleTabLeftChatClosed.isDown(keyEvent.key(), modifiers) -> {
                    selectedWindow.tabSettings.scrollTab(-1)
                    EventResult.interruptTrue()
                }

                Config.values.keyCycleTabRightChatClosed.isDown(keyEvent.key(), modifiers) -> {
                    selectedWindow.tabSettings.scrollTab(1)
                    EventResult.interruptTrue()
                }

                else -> EventResult.pass()
            }
        }
        EventBus.register<ChatScreenMouseScrolledEvent> {
            if (!Config.values.scrollCycleTabEnabled) {
                return@register
            }
            val amountX = it.amountX
            if (amountX == 0.0) {
                return@register
            }
            selectedWindow.tabSettings.scrollTab(Mth.clamp(amountX.toInt(), -1, 1))
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            if (selectedWindow.tabSettings.hideTabs) {
                return@register
            }
            val mouseX = it.mouseX
            val mouseY = it.mouseY
            if (isLeftMouseButton(it.button)) {
                selectedWindow.tabSettings.handleClickedTab(mouseX, mouseY)
            } else if (isRightMouseButton(it.button) && Config.values.tabEditorScreen) {
                val clickedTab = selectedWindow.tabSettings.getClickedTab(mouseX, mouseY) ?: return@register
                if (Config.values.windowEditorScreen && Minecraft.getInstance().hasShiftDown()) {
                    Minecraft.getInstance().setScreenAndShow(Editor.windowEditor(it.screen, selectedWindow))
                } else {
                    Minecraft.getInstance().setScreenAndShow(Editor.tabEditor(it.screen, clickedTab))
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


