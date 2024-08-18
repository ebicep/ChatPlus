package com.ebicep.chatplus.hud

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config.values
import com.ebicep.chatplus.config.MessageDirection
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_HEIGHT
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.hud.ChatManager.getLineHeight
import com.ebicep.chatplus.hud.ChatManager.getScale
import com.ebicep.chatplus.hud.ChatPlusScreen.EDIT_BOX_HEIGHT
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.mojang.blaze3d.vertex.PoseStack
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.client.GuiMessage
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth
import kotlin.math.ceil
import kotlin.math.roundToInt

abstract class ChatRenderLineEvent(
    open val guiGraphics: GuiGraphics,
    open val chatWindow: ChatWindow,
    open val chatPlusGuiMessageLine: ChatTab.ChatPlusGuiMessageLine,
    open val verticalChatOffset: Int,
    open val verticalTextOffset: Int,
) : Event {
    val line: GuiMessage.Line
        get() = chatPlusGuiMessageLine.line
}

class ChatRenderLineTextEvent(
    guiGraphics: GuiGraphics,
    chatWindow: ChatWindow,
    chatPlusGuiMessageLine: ChatTab.ChatPlusGuiMessageLine,
    val fadeOpacity: Double,
    val textColor: Int,
    val backgroundColor: Int,
    verticalChatOffset: Int,
    verticalTextOffset: Int,
    val text: String,
    val index: Int,
) : ChatRenderLineEvent(guiGraphics, chatWindow, chatPlusGuiMessageLine, verticalChatOffset, verticalTextOffset)

class ChatRenderPreLineAppearanceEvent(
    guiGraphics: GuiGraphics,
    chatWindow: ChatWindow,
    chatPlusGuiMessageLine: ChatTab.ChatPlusGuiMessageLine,
    verticalChatOffset: Int,
    verticalTextOffset: Int,
    var textColor: Int,
    var backgroundColor: Int,
) : ChatRenderLineEvent(guiGraphics, chatWindow, chatPlusGuiMessageLine, verticalChatOffset, verticalTextOffset)

data class ChatRenderPreLinesEvent(
    val guiGraphics: GuiGraphics,
    val chatWindow: ChatWindow,
    var chatFocused: Boolean,
    var returnFunction: Boolean = false
) : Event

data class ChatRenderPreLinesRenderEvent(
    val guiGraphics: GuiGraphics,
    val chatWindow: ChatWindow,
) : Event

data class ChatRenderPostLinesEvent(
    val guiGraphics: GuiGraphics,
    val chatWindow: ChatWindow,
    var displayMessageIndex: Int,
    var returnFunction: Boolean = false
) : Event

@Serializable
class ChatRenderer {

    var x: Int = 0
        set(newX) {
            if (field == newX) {
                return
            }
            field = newX
            internalX = newX
        }
    var y: Int = -CHAT_TAB_HEIGHT - EDIT_BOX_HEIGHT
        set(newY) {
            if (field == newY) {
                return
            }
            field = newY
            internalY = newY
        }
    var width: Int = MIN_WIDTH
        set(newWidth) {
            if (field == newWidth) {
                return
            }
            field = newWidth
            queueUpdateConfig = true
            internalWidth = newWidth
            chatWindow.tabs.forEach { it.rescaleChat() }
        }
    var height: Int = MIN_HEIGHT
        set(newHeight) {
            if (field == newHeight) {
                return
            }
            field = newHeight
            queueUpdateConfig = true
            internalHeight = newHeight
            updateCachedDimension()
        }

    @Transient
    var internalX: Int = 0

    @Transient
    var internalY: Int = -CHAT_TAB_HEIGHT - EDIT_BOX_HEIGHT

    @Transient
    var internalWidth: Int = MIN_WIDTH

    @Transient
    var internalHeight: Int = MIN_HEIGHT

    @Transient
    lateinit var chatWindow: ChatWindow

    @Transient
    private var previousScreenWidth = -1

    @Transient
    private var previousScreenHeight = -1

    // cached values since render is called every tick they only need to be calculated once/on change
    @Transient
    var textOpacity: Double = 0.0

    @Transient
    var backgroundOpacity: Float = 0f

    @Transient
    var lineSpacing: Float = 0f

    @Transient
    var l1 = 0

    @Transient
    var scale: Float = 0f

    @Transient
    var backgroundWidthEndX: Int = 0

    @Transient
    var rescaledX: Int = 0

    @Transient
    var rescaledY: Int = 0

    @Transient
    var rescaledHeight: Int = 0

    @Transient
    var rescaledWidth: Int = 0

    @Transient
    var rescaledEndX: Int = 0

    @Transient
    var rescaledLinesPerPage: Int = 0

    @Transient
    var lineHeight: Int = 0

    init {
        ChatPlus.LOGGER.info("ChatRenderer init")
        ChatPlus.LOGGER.info("x: $x, y: $y, width: $width, height: $height")
        internalX = x
        internalY = y
        internalWidth = width
        internalHeight = height
    }

    fun updateCachedDimension() {
        textOpacity = ChatManager.getTextOpacity() * 0.9 + 0.1
        backgroundOpacity = ChatManager.getBackgroundOpacity()
        lineSpacing = ChatManager.getLineSpacing()
        l1 = (-8.0 * (lineSpacing + 1.0) + 4.0 * lineSpacing).roundToInt()
        scale = getScale()
        internalX = getUpdatedX(x)
        internalY = getUpdatedY(y)
        internalHeight = getUpdatedHeight()
        internalWidth = getUpdatedWidth()
        backgroundWidthEndX = internalX + internalWidth
        rescaledX = ceil(internalX / scale).toInt()
        rescaledY = ceil(internalY / scale).toInt()
        rescaledHeight = ceil(internalHeight / scale).toInt()
        rescaledWidth = ceil(internalWidth / scale).toInt()
        rescaledEndX = ceil(backgroundWidthEndX / scale).toInt()
        rescaledLinesPerPage = getLinesPerPageScaled()
        lineHeight = getLineHeight()
    }

    fun render(chatWindow: ChatWindow, guiGraphics: GuiGraphics, guiTicks: Int, mouseX: Int, mouseY: Int) {
        if (internalY != getUpdatedY(y)) {
            updateCachedDimension()
        }
        handleScreenResize()

        val poseStack: PoseStack = guiGraphics.pose()
        var chatFocused: Boolean = ChatManager.isChatFocused()

        val preLinesEvent = ChatRenderPreLinesEvent(guiGraphics, chatWindow, chatFocused)
        if (EventBus.post(preLinesEvent).returnFunction) {
            return
        }
        chatFocused = preLinesEvent.chatFocused

        val messagesToDisplay = chatWindow.selectedTab.displayedMessages.size
        poseStack.pushPose()
        poseStack.scale(scale, scale, 1.0f)
        var displayMessageIndex = 0
        var linesPerPage = rescaledLinesPerPage
        if (!chatFocused) {
            linesPerPage = (linesPerPage * values.unfocusedHeight).roundToInt()
        }
        EventBus.post(ChatRenderPreLinesRenderEvent(guiGraphics, chatWindow))
        while (displayMessageIndex + chatWindow.selectedTab.chatScrollbarPos < messagesToDisplay && displayMessageIndex < linesPerPage) {
            val messageIndex = messagesToDisplay - displayMessageIndex - chatWindow.selectedTab.chatScrollbarPos
            val chatPlusGuiMessageLine: ChatTab.ChatPlusGuiMessageLine = chatWindow.selectedTab.displayedMessages[messageIndex - 1]
            val line: GuiMessage.Line = chatPlusGuiMessageLine.line
            val ticksLived: Int = guiTicks - line.addedTime()
            if (ticksLived >= 200 && !chatFocused) {
                ++displayMessageIndex
                continue
            }
            val fadeOpacity = if (chatFocused) 1.0 else getTimeFactor(ticksLived)
            var textColor = (255.0 * fadeOpacity * textOpacity).toInt()
            var backgroundColor = chatWindow.backgroundColor
            if (textColor <= 3) {
                ++displayMessageIndex
                continue
            }
            // how high chat is from input bar, if changed need to change queue offset
            val verticalChatOffset: Int = when (values.messageDirection) {
                MessageDirection.TOP_DOWN -> (rescaledY - rescaledLinesPerPage * lineHeight + lineHeight) + displayMessageIndex * lineHeight
                MessageDirection.BOTTOM_UP -> rescaledY - displayMessageIndex * lineHeight
            }
            val verticalTextOffset: Int = verticalChatOffset + l1 // align text with background
            val lineAppearanceEvent = ChatRenderPreLineAppearanceEvent(
                guiGraphics,
                chatWindow,
                chatPlusGuiMessageLine,
                verticalChatOffset,
                verticalTextOffset,
                textColor,
                backgroundColor
            )
            EventBus.post(lineAppearanceEvent)
            textColor = lineAppearanceEvent.textColor
            backgroundColor = lineAppearanceEvent.backgroundColor
            val oldAlpha = (backgroundColor shr 24) and 0xff
            val newAlpha = oldAlpha * fadeOpacity
            backgroundColor = (backgroundColor and 0x00ffffff) or (newAlpha.toInt() shl 24)
            poseStack.createPose {
                poseStack.guiForward(amount = 50.0)
                //background
                guiGraphics.fill(
                    rescaledX,
                    verticalChatOffset - lineHeight,
                    rescaledEndX,
                    verticalChatOffset,
                    backgroundColor
                )
            }
            poseStack.createPose {
                poseStack.guiForward(amount = 100.0)
                EventBus.post(
                    ChatRenderLineTextEvent(
                        guiGraphics,
                        chatWindow,
                        chatPlusGuiMessageLine,
                        fadeOpacity,
                        textColor,
                        backgroundColor,
                        verticalChatOffset,
                        verticalTextOffset,
                        chatPlusGuiMessageLine.content,
                        messageIndex
                    )
                )
                // text
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    line.content,
                    rescaledX,
                    verticalTextOffset,
                    16777215 + (textColor shl 24)
                )
            }
            ++displayMessageIndex
        }
        if (EventBus.post(ChatRenderPostLinesEvent(guiGraphics, chatWindow, displayMessageIndex)).returnFunction) {
            return
        }
        poseStack.popPose()
    }

    private fun handleScreenResize() {
        val screenWidth = Minecraft.getInstance().window.guiScaledWidth
        val screenHeight = Minecraft.getInstance().window.guiScaledHeight

        val widthChanged = screenWidth != previousScreenWidth && previousScreenWidth != -1
        val heightChanged = screenHeight != previousScreenHeight && previousScreenHeight != -1

        if (widthChanged) {
            internalX = x
            internalWidth = width
            getUpdatedX()
            if (screenWidth < previousScreenWidth) {
                internalX = (screenWidth * internalX / previousScreenWidth.toDouble()).roundToInt()
            }
        }
        if (heightChanged) {
            internalY = y
            internalHeight = height
            getUpdatedY()
        }
        previousScreenWidth = screenWidth
        previousScreenHeight = screenHeight

        if (heightChanged || widthChanged) {
            updateCachedDimension()
        }
    }

    private fun getTimeFactor(ticksLived: Int): Double {
        var d0 = ticksLived.toDouble() / 200.0
        d0 = 1.0 - d0
        d0 *= 10.0
        d0 = Mth.clamp(d0, 0.0, 1.0)
        return d0 * d0
    }

    /**
     * Width of chat window, raw value not scaled
     */
    fun getUpdatedWidth(): Int {
        var width = internalWidth
        val guiWidth = Minecraft.getInstance().window.guiScaledWidth
        val lowerThanMin = width < MIN_WIDTH
        val x = internalX
        val hasSpace = guiWidth - x >= MIN_WIDTH
        if (lowerThanMin && hasSpace) {
            width = MIN_WIDTH
            chatWindow.selectedTab.rescaleChat()
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
        return getUpdatedWidth() / getScale()
    }

    /**
     * Height of chat window, raw value not scaled
     */
    fun getUpdatedHeight(): Int {
        var height = internalHeight
        val lowerThanMin = height < MIN_HEIGHT
        val hasSpace = internalY - 1 >= MIN_HEIGHT
        if (lowerThanMin && hasSpace) {
            height = MIN_HEIGHT
        }
        if (internalY - height <= 0) {
            height = internalY - 1
        }
        if (height >= internalY) {
            height = internalY - 1
        }
        return height
    }

    fun getUpdatedX(): Int {
        return getUpdatedX(internalX)
    }

    fun getUpdatedX(startingX: Int): Int {
        var x = startingX
        if (x + internalWidth >= Minecraft.getInstance().window.guiScaledWidth) {
            x = Minecraft.getInstance().window.guiScaledWidth - internalWidth - 1
        }
        if (x < 0) {
            x = 0
        }
        return x
    }

    fun getUpdatedY(): Int {
        val updatedY = getUpdatedY(internalY)
        if (updatedY == getDefaultY()) {
            internalY = getDefaultY()
        }
        return updatedY
    }

    fun getUpdatedY(startingY: Int): Int {
        var y = startingY
        if (y < 0) {
            y += Minecraft.getInstance().window.guiScaledHeight
        }
        if (y >= Minecraft.getInstance().window.guiScaledHeight - EDIT_BOX_HEIGHT) {
            y = getMaxHeightScaled()
        }
        return y
    }

    fun getLinesPerPage(): Int {
        return getUpdatedHeight() / getLineHeight()
    }

    fun getLinesPerPageScaled(): Int {
        return (getLinesPerPage() / getScale()).roundToInt()
    }

    fun getDefaultY(): Int {
        return EventBus.post(GetDefaultYEvent(chatWindow, -EDIT_BOX_HEIGHT)).y
    }

    fun getMaxWidthScaled(): Int {
        return EventBus.post(GetMaxWidthEvent(chatWindow, Minecraft.getInstance().window.guiScaledWidth)).maxWidth
    }

    fun getMaxHeightScaled(): Int {
        return EventBus.post(GetMaxHeightEvent(chatWindow, Minecraft.getInstance().window.guiScaledHeight - EDIT_BOX_HEIGHT)).maxHeight
    }

    fun getMaxHeightScaled(guiScaledHeight: Int): Int {
        return EventBus.post(GetMaxHeightEvent(chatWindow, guiScaledHeight - EDIT_BOX_HEIGHT)).maxHeight
    }

    fun getMinWidthScaled(): Int {
        return MIN_WIDTH
//        return (MIN_WIDTH / getScale()).roundToInt()
    }

    fun getMinHeightScaled(): Int {
        return MIN_HEIGHT
//        return (MIN_HEIGHT / getScale()).roundToInt()
    }


}