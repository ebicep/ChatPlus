package com.ebicep.chatplus.hud

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.ChatTabs.DefaultTab
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.hud.ChatPlusScreen.EDIT_BOX_HEIGHT
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen

const val MIN_HEIGHT = 80
const val MIN_WIDTH = 160

data class GetMaxWidthEvent(
    var maxWidth: Int
) : Event

data class GetDefaultYEvent(
    var y: Int
) : Event

data class GetMaxHeightEvent(
    var maxHeight: Int
) : Event

object ChatManager {

    val sentMessages: MutableList<String> = ArrayList()
    val selectedWindow: ChatWindow
        get() = Config.values.chatWindows.last()
    val globalSelectedTab: ChatTab
        get() {
            if (!Config.values.chatTabsEnabled) {
                return DefaultTab
            }
            if (selectedWindow.tabs.isEmpty()) {
                selectedWindow.tabs.add(DefaultTab)
                selectedWindow.selectedTabIndex = 0
                queueUpdateConfig = true
            }
            return selectedWindow.selectedTab
        }

    init {
        sentMessages.addAll(Minecraft.getInstance().commandHistory().history())
    }

    fun getDefaultY(): Int {
        return EventBus.post(GetDefaultYEvent(-EDIT_BOX_HEIGHT)).y
    }

    fun getMaxWidthScaled(): Int {
        return EventBus.post(GetMaxWidthEvent(Minecraft.getInstance().window.guiScaledWidth)).maxWidth
    }

    fun getMaxHeightScaled(): Int {
        return EventBus.post(GetMaxHeightEvent(Minecraft.getInstance().window.guiScaledHeight - EDIT_BOX_HEIGHT)).maxHeight
    }

    fun getMaxHeightScaled(guiScaledHeight: Int): Int {
        return EventBus.post(GetMaxHeightEvent(guiScaledHeight - EDIT_BOX_HEIGHT)).maxHeight
    }

    fun getMinWidthScaled(): Int {
        return MIN_WIDTH
//        return (MIN_WIDTH / getScale()).roundToInt()
    }

    fun getMinHeightScaled(): Int {
        return MIN_HEIGHT
//        return (MIN_HEIGHT / getScale()).roundToInt()
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

    fun getScale(): Float {
        val scale = Config.values.scale
        if (scale <= 0) {
            return .001f
        }
        return scale
    }

    fun getLineHeight(): Int {
        return (9.0 * (getLineSpacing() + 1.0)).toInt()
    }

    fun getTextOpacity(): Float {
        return Config.values.textOpacity
    }

    fun getBackgroundOpacity(): Float {
        return Config.values.backgroundOpacity
    }

    fun getLineSpacing(): Float {
        return Config.values.lineSpacing
    }

}