package com.ebicep.chatplus.hud

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.ChatTabs.defaultTab
import net.minecraft.client.Minecraft
import kotlin.math.roundToInt

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
    val selectedTab: ChatTab
        get() {
            if (!Config.values.chatTabsEnabled) {
                return defaultTab
            }
            if (Config.values.chatTabs.isEmpty()) {
                Config.values.chatTabs.add(defaultTab)
                Config.values.selectedTab = 0
                queueUpdateConfig = true
            }
            return Config.values.chatTabs[Config.values.selectedTab]
        }

    init {
//        sentMessages.addAll(Minecraft.getInstance().commandHistory().history())
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
//        if (pMessage.startsWith("/")) {
//            Minecraft.getInstance().commandHistory().addCommand(pMessage)
//        }
    }

    fun isChatFocused(): Boolean {
        return Minecraft.getInstance().screen is ChatPlusScreen
    }

    fun getScale(): Float {
        val scale = Config.values.scale
        if (scale <= 0) {
            return .001f
        }
        return scale
    }

    /**
     * Width of chat window, raw value not scaled
     */
    fun getWidth(): Int {
        var width = Config.values.width
        val guiWidth = Minecraft.getInstance().window.guiScaledWidth
        val lowerThanMin = width < MIN_WIDTH
        val x = ChatRenderer.x
        val hasSpace = guiWidth - x >= MIN_WIDTH
        if (lowerThanMin && hasSpace) {
            width = MIN_WIDTH
            selectedTab.rescaleChat()
        }
        if (width <= 0) {
            width = 200.coerceAtMost(guiWidth - x - 1)
        }
        if (x + width >= guiWidth) {
            width = guiWidth - x
        }
        return width
    }

    fun getBackgroundWidth(): Float {
        return getWidth() / getScale()
    }


    /**
     * Height of chat window, raw value not scaled
     */
    fun getHeight(): Int {
        var height = Config.values.height
        val lowerThanMin = Config.values.height < MIN_HEIGHT
        val y = ChatRenderer.y
        val hasSpace = y - 1 >= MIN_HEIGHT
        if (lowerThanMin && hasSpace) {
            height = MIN_HEIGHT
            selectedTab.rescaleChat()
        }
        if (y - Config.values.height <= 0) {
            height = y - 1
        }
        if (height >= y) {
            height = y - 1
        }
        return height
    }

    fun getX(): Int {
        var x = Config.values.x
        if (x < 0) {
            x = 0
            Config.values.x = x
        }
        if (x >= Minecraft.getInstance().window.guiScaledWidth) {
            x = Minecraft.getInstance().window.guiScaledWidth - 1
            Config.values.x = x
        }
        return x
    }

    fun getY(): Int {
        var y = Config.values.y
        if (y < 0) {
            y += Minecraft.getInstance().window.guiScaledHeight
        }
        if (y >= Minecraft.getInstance().window.guiScaledHeight - EDIT_BOX_HEIGHT) {
            y = getMaxHeightScaled()
            Config.values.y = getDefaultY()
        }
        return y
    }

    fun getLinesPerPage(): Int {
        return getHeight() / getLineHeight()
    }

    fun getLinesPerPageScaled(): Int {
        return (getLinesPerPage() / getScale()).roundToInt()
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