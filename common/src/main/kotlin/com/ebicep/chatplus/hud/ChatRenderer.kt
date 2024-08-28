package com.ebicep.chatplus.hud

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.MessageDirection
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_HEIGHT
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.features.internal.Debug
import com.ebicep.chatplus.hud.ChatPlusScreen.EDIT_BOX_HEIGHT
import com.ebicep.chatplus.hud.ChatPlusScreen.lastMouseX
import com.ebicep.chatplus.hud.ChatPlusScreen.lastMouseY
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.drawString0
import com.ebicep.chatplus.util.GraphicsUtil.fill0
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.KotlinUtil.reduceAlpha
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
    open val verticalChatOffset: Float,
    open val verticalTextOffset: Float,
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
    verticalChatOffset: Float,
    verticalTextOffset: Float,
    val text: String,
    val index: Int,
) : ChatRenderLineEvent(guiGraphics, chatWindow, chatPlusGuiMessageLine, verticalChatOffset, verticalTextOffset)

class ChatRenderPreLineAppearanceEvent(
    guiGraphics: GuiGraphics,
    chatWindow: ChatWindow,
    chatPlusGuiMessageLine: ChatTab.ChatPlusGuiMessageLine,
    verticalChatOffset: Float,
    verticalTextOffset: Float,
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
        }

    @Transient
    var internalX: Int = 0

    @Transient
    var internalY: Int = -CHAT_TAB_HEIGHT - EDIT_BOX_HEIGHT

    @Transient
    var internalWidth: Int = MIN_WIDTH

    @Transient
    var internalHeight: Int = MIN_HEIGHT
        set(newHeight) {
            val lineHeightScaled = lineHeight * scale
            field = newHeight//(newHeight - (newHeight % lineHeightScaled)).toInt()
        }

    @Transient
    lateinit var chatWindow: ChatWindow

    @Transient
    private var previousScreenWidth = -1

    @Transient
    private var previousScreenHeight = -1

    // cached values since render is called every tick they only need to be calculated once/on change
    @Transient
    var l1 = 0

    @Transient
    var scale: Float = 0f

    @Transient
    var backgroundWidthEndX: Int = 0

    @Transient
    var rescaledX: Float = 0f

    @Transient
    var rescaledY: Float = 0f

    @Transient
    var rescaledHeight: Int = 0

    @Transient
    var rescaledWidth: Int = 0

    @Transient
    var rescaledEndX: Float = 0f

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
        l1 = (-8.0 * (chatWindow.lineSpacing + 1.0) + 4.0 * chatWindow.lineSpacing).roundToInt()
        scale = getUpdatedScale()
        internalX = getUpdatedX(x)
        internalY = getUpdatedY(y)
        internalHeight = getUpdatedHeight(height)
        val updatedWidth = getUpdatedWidth(width)
        internalWidth = updatedWidth.newWidth
        val updateWidthStatus = updatedWidth.status
        if (updateWidthStatus != UpdateWidthStatus.SUCCESS) {
            if (updateWidthStatus == UpdateWidthStatus.LOWER_MIN_WITH_SPACE || updateWidthStatus == UpdateWidthStatus.LESS_THAN_ZERO) {
                width = MIN_WIDTH
            }
            chatWindow.selectedTab.rescaleChat()
        }
        backgroundWidthEndX = internalX + internalWidth
        rescaledX = internalX / scale
        rescaledY = internalY / scale
        rescaledHeight = ceil(internalHeight / scale).toInt()
        rescaledWidth = ceil(internalWidth / scale).toInt()
        rescaledEndX = backgroundWidthEndX / scale
        rescaledLinesPerPage = getLinesPerPageScaled()
        lineHeight = getUpdatedLineHeight()
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
            linesPerPage = (linesPerPage * chatWindow.unfocusedHeight).roundToInt()
        }
        EventBus.post(ChatRenderPreLinesRenderEvent(guiGraphics, chatWindow))
        val textOpacity = chatWindow.getUpdatedTextOpacity()
        val updatedBackgroundColor = chatWindow.getUpdatedBackgroundColor()
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
            val textOpacity = (255.0 * fadeOpacity * textOpacity).toInt()
            var backgroundColor = updatedBackgroundColor
            // how high chat is from input bar, if changed need to change queue offset
            val verticalChatOffset: Float = when (chatWindow.messageDirection) {
                MessageDirection.TOP_DOWN -> (rescaledY - rescaledLinesPerPage * lineHeight + lineHeight) + displayMessageIndex * lineHeight
                MessageDirection.BOTTOM_UP -> rescaledY - displayMessageIndex * lineHeight
            }
            val verticalTextOffset: Float = verticalChatOffset + l1 // align text with background
            val lineAppearanceEvent = ChatRenderPreLineAppearanceEvent(
                guiGraphics,
                chatWindow,
                chatPlusGuiMessageLine,
                verticalChatOffset,
                verticalTextOffset,
                16777215 + (textOpacity shl 24),
                backgroundColor
            )
            EventBus.post(lineAppearanceEvent)
            val textColor = lineAppearanceEvent.textColor
            backgroundColor = reduceAlpha(lineAppearanceEvent.backgroundColor, fadeOpacity)
            poseStack.createPose {
                poseStack.guiForward(amount = 50.0)
                //background
                guiGraphics.fill0(
                    internalX / scale,
                    verticalChatOffset - lineHeight.toFloat(),
                    rescaledEndX,
                    verticalChatOffset,
                    200,
                    backgroundColor
                )
            }
            if (textOpacity <= 3) {
                ++displayMessageIndex
                continue
            }
            poseStack.createPose {
                poseStack.guiForward(amount = 300.0)
                EventBus.post(
                    ChatRenderLineTextEvent(
                        guiGraphics,
                        chatWindow,
                        chatPlusGuiMessageLine,
                        fadeOpacity,
                        textOpacity,
                        backgroundColor,
                        verticalChatOffset,
                        verticalTextOffset,
                        chatPlusGuiMessageLine.content,
                        messageIndex
                    )
                )
                // text
                guiGraphics.drawString0(
                    Minecraft.getInstance().font,
                    line.content,
                    rescaledX,
                    verticalTextOffset,
                    textColor
                )
            }
            ++displayMessageIndex
        }
        if (EventBus.post(ChatRenderPostLinesEvent(guiGraphics, chatWindow, displayMessageIndex)).returnFunction) {
            return
        }
        poseStack.popPose()

        if (chatFocused && Debug.debug && chatWindow == ChatManager.selectedWindow) {
            poseStack.createPose {
                poseStack.guiForward(amount = 500.0)
                guiGraphics.drawString(Minecraft.getInstance().font, "$height", lastMouseX - 15, lastMouseY + 5, 0x3eeff)
                guiGraphics.drawString(Minecraft.getInstance().font, "$rescaledHeight", lastMouseX - 15, lastMouseY + 15, 0x3eeff)
                guiGraphics.drawString(Minecraft.getInstance().font, "$lineHeight", lastMouseX - 15, lastMouseY + 25, 0x3eeff)
                guiGraphics.drawString(Minecraft.getInstance().font, "${getLinesPerPage()}", lastMouseX - 15, lastMouseY + 35, 0x3eeff)
                guiGraphics.drawString(Minecraft.getInstance().font, "$rescaledLinesPerPage", lastMouseX - 15, lastMouseY + 45, 0x3eeff)
            }
        }
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
            if (widthChanged) {
                chatWindow.tabs.forEach { it.rescaleChat() }
            }
        }
    }

    private fun getTimeFactor(ticksLived: Int): Double {
        var d0 = ticksLived.toDouble() / 200.0
        d0 = 1.0 - d0
        d0 *= 10.0
        d0 = Mth.clamp(d0, 0.0, 1.0)
        return d0 * d0
    }

    fun getUpdatedWidth(): UpdateWidth {
        return getUpdatedWidth(internalWidth)
    }

    fun getUpdatedWidthValue(): Int {
        return getUpdatedWidth().newWidth
    }

    fun getUpdatedWidth(startingWidth: Int): UpdateWidth {
        var w = startingWidth
        val guiWidth = Minecraft.getInstance().window.guiScaledWidth
        val lowerThanMin = w < MIN_WIDTH
        val x = internalX
        val hasSpace = guiWidth - x >= MIN_WIDTH
        var status = UpdateWidthStatus.SUCCESS
        if (lowerThanMin && hasSpace) {
            w = MIN_WIDTH
            status = UpdateWidthStatus.LOWER_MIN_WITH_SPACE
        }
        if (w <= 0) {
            w = MIN_WIDTH.coerceAtMost(guiWidth - x)
            status = UpdateWidthStatus.LESS_THAN_ZERO
        }
        if (x + w >= guiWidth) {
            w = guiWidth - x
            status = UpdateWidthStatus.GREATER_THAN_GUI_WIDTH
        }
        return UpdateWidth(status, w)
    }

    data class UpdateWidth(val status: UpdateWidthStatus, val newWidth: Int)

    enum class UpdateWidthStatus {
        LOWER_MIN_WITH_SPACE,
        LESS_THAN_ZERO,
        GREATER_THAN_GUI_WIDTH,
        SUCCESS
    }

    fun getBackgroundWidth(): Float {
        return getUpdatedWidthValue() / getUpdatedScale()
    }

    fun getUpdatedHeight(): Int {
        return getUpdatedHeight(internalHeight)
    }

    fun getUpdatedHeight(startingHeight: Int): Int {
        var h = startingHeight
        val lowerThanMin = h < MIN_HEIGHT
        val hasSpace = internalY - 1 >= MIN_HEIGHT
        if (lowerThanMin && hasSpace) {
            h = MIN_HEIGHT
        }
        if (internalY - h <= 0) {
            h = internalY - 1
        }
        if (h >= internalY) {
            h = internalY - 1
        }
        return h
    }

    fun getUpdatedX(): Int {
        return getUpdatedX(internalX)
    }

    fun getUpdatedX(startingX: Int): Int {
        var x = startingX
        if (x + internalWidth >= Minecraft.getInstance().window.guiScaledWidth) {
            x = Minecraft.getInstance().window.guiScaledWidth - internalWidth
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

    fun getUpdatedLineHeight(): Int {
        return (9.0 * (chatWindow.lineSpacing + 1.0)).toInt()
    }

    fun getLinesPerPage(): Int {
        return (getUpdatedHeight() / getUpdatedLineHeight().toDouble()).roundToInt()
    }

    fun getLinesPerPageScaled(): Int {
        return (getUpdatedHeight() / getUpdatedLineHeight().toDouble() / getUpdatedScale()).roundToInt()
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

    fun getUpdatedScale(): Float {
        val scale = chatWindow.scale
        if (scale <= 0) {
            return .001f
        }
        return scale
    }

}