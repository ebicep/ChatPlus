package com.ebicep.chatplus.hud

import com.ebicep.chatplus.config.Config
import net.minecraft.client.Minecraft
import kotlin.math.roundToInt

const val baseYOffset = 29
const val minHeight = 80
const val minWidth = 160

object ChatManager {

    val sentMessages: MutableList<String> = ArrayList()
    val selectedTab: ChatTab
        get() {
            if (Config.values.chatTabs.isEmpty()) {
                Config.values.chatTabs.add(ChatTab("All", "(?s).*"))
                Config.values.selectedTab = 0
                Config.save()
            }
            return Config.values.chatTabs[Config.values.selectedTab]
        }

    fun getMinWidthScaled(): Int {
        return (minWidth / getScale()).roundToInt()
    }

    fun getMinHeightScaled(): Int {
        return (minHeight / getScale()).roundToInt()
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
        if (this.sentMessages.isEmpty() || this.sentMessages[this.sentMessages.size - 1] != pMessage) {
            this.sentMessages.add(pMessage)
        }
    }

    fun handleClickedCategory(x: Double, y: Double) {
        val translatedY = getY() - y
        var xOff = 0.0
        val font = Minecraft.getInstance().font
        //ChatPlus.LOGGER.debug("x: $x, translatedY: $translatedY")
        if (translatedY > tabYOffset || translatedY < -(9 + ChatTab.PADDING + ChatTab.PADDING)) {
            return
        }
        Config.values.chatTabs.forEachIndexed { index, it ->
            val categoryLength = font.width(it.name) + ChatTab.PADDING + ChatTab.PADDING
            if (x > xOff && x < xOff + categoryLength && it != selectedTab) {
                Config.values.selectedTab = index
                Config.save()
                selectedTab.refreshDisplayedMessage()
                return
            }
            xOff += categoryLength + tabXBetween
        }
    }

    fun isChatFocused(): Boolean {
        return Minecraft.getInstance().screen is ChatPlusScreen
    }

    fun getScale(): Float {
        return Config.values.scale
    }

    /**
     * Width of chat window, raw value not scaled
     */
    fun getWidth(): Int {
        val guiWidth = Minecraft.getInstance().window.guiScaledWidth
        val minWidthScaled = getMinWidthScaled()
        val lowerThanMin = Config.values.chatWidth < minWidthScaled
        val hasSpace = guiWidth - getX() - 1 >= minWidthScaled
        if (lowerThanMin && hasSpace) {
            Config.values.chatWidth = minWidthScaled
            selectedTab.rescaleChat()
        }
        if (Config.values.chatWidth <= 0) {
            Config.values.chatWidth = 200.coerceAtMost(guiWidth - getX() - 1)
        }
        if (getX() + Config.values.chatWidth >= guiWidth) {
            Config.values.chatWidth = guiWidth - getX() - 1
        }
        return Config.values.chatWidth
    }

    fun getBackgroundWidth(): Float {
        return getWidth() / getScale()
    }


    /**
     * Height of chat window, raw value not scaled
     */
    fun getHeight(): Int {
        val minHeightScaled = getMinHeightScaled()
        val lowerThanMin = Config.values.chatHeight < minHeightScaled
        val hasSpace = getY() - 1 >= minHeightScaled
        if (lowerThanMin && hasSpace) {
            Config.values.chatHeight = minHeightScaled
            selectedTab.rescaleChat()
        }
        if (getY() - Config.values.chatHeight <= 0) {
            Config.values.chatHeight = getY() - 1
        }
        if (Config.values.chatHeight >= getY()) {
            Config.values.chatHeight = getY() - 1
        }
        return if (isChatFocused()) {
            Config.values.chatHeight
        } else {
            (Config.values.chatHeight * .5).roundToInt()
        }
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
        if (y >= Minecraft.getInstance().window.guiScaledHeight) {
            y = Minecraft.getInstance().window.guiScaledHeight - baseYOffset
            Config.values.y = -baseYOffset
        }
        return y
    }

//    /**
//     * Y offset from bottom, all values should be negative
//     */
//    fun getYOffset(): Int {
//        var y = Config.values.y
//        if (y > 0) {
//            y = -baseYOffset
//            Config.values.y = y
//        }
//    }


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