package com.ebicep.chatplus.hud

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.ChatTabs.DefaultTab
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen

const val MIN_HEIGHT = 8
const val MIN_WIDTH = 130

object ChatManager {

    val sentMessages: MutableList<String> = ArrayList()
    val selectedWindow: ChatWindow
        get() = Config.values.chatWindows.last()
    val globalSelectedTab: ChatTab
        get() {
            if (!Config.values.chatWindowsTabsEnabled) {
                return DefaultTab
            }
            if (selectedWindow.tabSettings.tabs.isEmpty()) {
                selectedWindow.tabSettings.tabs.add(DefaultTab)
                selectedWindow.tabSettings.selectedTabIndex = 0
                queueUpdateConfig = true
            }
            return selectedWindow.tabSettings.selectedTab
        }

    init {
        sentMessages.addAll(Minecraft.getInstance().commandHistory().history())
    }
    /**
     * Gets the list of messages previously sent through the chat GUI
     */
    fun getRecentChat(): List<String?> {
        return this.sentMessages
    }

    /**
     * Adds this string to the list of sent messages, for recall using the up/down arrow keys
     */
    fun addSentMessage(pMessage: String) {
        ChatPlusScreen.lastMessageSentTick = Events.currentTick
        if (this.sentMessages.isEmpty() || this.sentMessages[this.sentMessages.size - 1] != pMessage) {
            this.sentMessages.add(pMessage)
        }
        if (pMessage.startsWith("/")) {
            Minecraft.getInstance().commandHistory().addCommand(pMessage)
        }
    }

    fun isChatFocused(): Boolean {
        return Minecraft.getInstance().screen is ChatScreen
    }

    fun rescaleAll() {
        Config.values.chatWindows.forEach { window -> window.tabSettings.tabs.forEach { tab -> tab.rescaleChat() } }
    }

}

data class GetMaxWidthEvent(
    var chatWindow: ChatWindow,
    var maxWidth: Int
) : Event

data class GetDefaultYEvent(
    var chatWindow: ChatWindow,
    var y: Int
) : Event

data class GetMaxHeightEvent(
    var chatWindow: ChatWindow,
    val heightType: HeightType,
    var maxHeight: Int
) : Event

data class GetMaxYEvent(
    var chatWindow: ChatWindow,
    var maxY: Int
) : Event
