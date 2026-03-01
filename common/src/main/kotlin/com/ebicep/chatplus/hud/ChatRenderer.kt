package com.ebicep.chatplus.hud

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.AnchorPoint
import com.ebicep.chatplus.config.Config
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
import com.ebicep.chatplus.util.GraphicsUtil
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
import kotlin.math.max
import kotlin.math.min
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
    val guiTicks: Int,
) : Event

data class ChatRenderPostLinesEvent(
    val guiGraphics: GuiGraphics,
    val chatWindow: ChatWindow,
    var displayMessageIndex: Int,
    var returnFunction: Boolean = false
) : Event

enum class HeightType {
    RAW, // raw height

    ADJUSTED, // raw height minus bottom padding
    RENDERED_LINES, // height of rendered lines (minus bottom/top padding)
}

data class GetHeightEvent(val chatWindow: ChatWindow, var startingHeight: Int, val heightType: HeightType)

enum class UpdateWidthStatus {
    LOWER_MIN_WITH_SPACE,
    LESS_THAN_ZERO,
    GREATER_THAN_GUI_WIDTH,
    SUCCESS
}

data class UpdateWidth(val status: UpdateWidthStatus, val newWidth: Int)

data class GetTotalLineHeightEvent(val chatWindow: ChatWindow, var totalLineHeight: Float)

@Serializable
class ChatRenderer {

    // Stored as anchor-relative distance (see AnchorPoint docs):
    //   LEFT/TOP anchors:  positive = pixels from that edge
    //   RIGHT/BOTTOM:      negative = -pixels from that edge
    //   CENTER:            signed offset from screen center to window center
    var x: Int = 0
        set(absoluteX) {
            // Convert the incoming absolute screen coordinate to anchor-relative storage.
            // Falls back to absolute if chatWindow or Minecraft aren't ready yet (e.g. during init).
            val anchorX = runCatching {
                chatWindow.generalSettings.anchorPoint.absoluteToAnchorX(
                    absoluteX,
                    Minecraft.getInstance().window.guiScaledWidth,
                    internalWidth
                )
            }.getOrElse { absoluteX }
            if (field == anchorX) {
                return
            }
            field = anchorX
            internalX = absoluteX
        }
    var y: Int = -CHAT_TAB_HEIGHT - EDIT_BOX_HEIGHT
        set(absoluteY) {
            // Convert the incoming absolute screen coordinate to anchor-relative storage.
            val anchorY = runCatching {
                chatWindow.generalSettings.anchorPoint.absoluteToAnchorY(
                    absoluteY,
                    Minecraft.getInstance().window.guiScaledHeight,
                    internalHeight
                )
            }.getOrElse { absoluteY }
            if (field == anchorY) {
                return
            }
            field = anchorY
            internalY = absoluteY
        }
    var width: Int = 80
        set(newWidth) {
            if (field == newWidth) {
                return
            }
            field = newWidth
            queueUpdateConfig = true
            internalWidth = newWidth
            chatWindow.tabSettings.tabs.forEach { it.rescaleChat() }
        }
    var height: Int = 160
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
    var internalWidth: Int = 80

    @Transient
    var internalHeight: Int = 160

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
        l1 = (-8.0 * (chatWindow.generalSettings.lineSpacing + 1.0) + 4.0 * chatWindow.generalSettings.lineSpacing).roundToInt()
        scale = getUpdatedScale()
        lineHeight = getUpdatedLineHeight()
        internalX = computeAnchorInternalX()
        internalY = computeAnchorInternalY()
        internalHeight = getUpdatedHeight(height, HeightType.RAW)
        val updatedWidth = getUpdatedWidth(width)
        internalWidth = updatedWidth.newWidth
        val updateWidthStatus = updatedWidth.status
        if (updateWidthStatus != UpdateWidthStatus.SUCCESS) {
            if (updateWidthStatus == UpdateWidthStatus.LOWER_MIN_WITH_SPACE || updateWidthStatus == UpdateWidthStatus.LESS_THAN_ZERO) {
                width = MIN_WIDTH
            }
            chatWindow.tabSettings.selectedTab.rescaleChat()
        }
        backgroundWidthEndX = internalX + internalWidth
        rescaledX = internalX / scale
        rescaledY = internalY / scale
        rescaledHeight = ceil(internalHeight / scale).toInt()
        rescaledWidth = ceil(internalWidth / scale).toInt()
        rescaledEndX = backgroundWidthEndX / scale
        rescaledLinesPerPage = getLinesPerPageScaled(HeightType.RENDERED_LINES)
    }

    fun render(chatWindow: ChatWindow, guiGraphics: GuiGraphics, guiTicks: Int, mouseX: Int, mouseY: Int) {
        if (EventBus.post(RenderValidateYEvent(this, internalY)).internalY != computeAnchorInternalY()) {
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

        val messagesToDisplay = chatWindow.tabSettings.selectedTab.displayedMessages.size
        poseStack.pushPose()
        poseStack.scale(scale, scale, 1.0f)
        var displayMessageIndex = 0
        var linesPerPage = rescaledLinesPerPage
        if (!chatFocused) {
            linesPerPage = (linesPerPage * chatWindow.generalSettings.unfocusedHeight).roundToInt()
        }
        EventBus.post(ChatRenderPreLinesRenderEvent(guiGraphics, chatWindow, guiTicks))
        val updatedTextOpacity = chatWindow.generalSettings.getUpdatedTextOpacity()
        val updatedBackgroundColor = chatWindow.generalSettings.getUpdatedBackgroundColor()
        while (displayMessageIndex + chatWindow.tabSettings.selectedTab.chatScrollbarPos < messagesToDisplay && displayMessageIndex < linesPerPage) {
            val messageIndex = messagesToDisplay - displayMessageIndex - chatWindow.tabSettings.selectedTab.chatScrollbarPos
            val chatPlusGuiMessageLine = chatWindow.tabSettings.selectedTab.displayedMessages.getOrNull(messageIndex - 1)
            if (chatPlusGuiMessageLine == null) {
                ++displayMessageIndex
                continue
            }
            val line: GuiMessage.Line = chatPlusGuiMessageLine.line
            val ticksLived: Int = guiTicks - line.addedTime()
            if (ticksLived >= 200 && !chatFocused) {
                ++displayMessageIndex
                continue
            }
            val fadeOpacity = if (chatFocused) 1.0 else getTimeFactor(ticksLived)
            val textOpacity = (255.0 * fadeOpacity * updatedTextOpacity).toInt()
            var backgroundColor = updatedBackgroundColor
            // how high chat is from input bar, if changed need to change queue offset
            val verticalChatOffset: Float = when (chatWindow.generalSettings.messageDirection) {
                MessageDirection.TOP_DOWN -> (rescaledY - rescaledLinesPerPage * lineHeight + lineHeight) + displayMessageIndex * lineHeight
                MessageDirection.BOTTOM_UP -> rescaledY - displayMessageIndex * lineHeight
            }
            val verticalTextOffset: Float = verticalChatOffset + l1 // align text with background
            var textColor: Int = 16777215 + (textOpacity shl 24)
            poseStack.createPose {
                poseStack.guiForward()
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
                backgroundColor = reduceAlpha(lineAppearanceEvent.backgroundColor, fadeOpacity)
                //background
                guiGraphics.fill0(
                    internalX / scale,
                    verticalChatOffset - lineHeight.toFloat(),
                    rescaledEndX,
                    verticalChatOffset,
                    backgroundColor
                )
            }
            if (textOpacity <= 3) {
                ++displayMessageIndex
                continue
            }
            poseStack.createPose {
                poseStack.guiForward()
                poseStack.guiForward()
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
                    line.content,
                    rescaledX,
                    verticalTextOffset,
                    textColor,
                    chatWindow.generalSettings.textShadow
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
                poseStack.guiForward(GraphicsUtil.GuiForwardType.ChatRendererDebug)
                guiGraphics.drawString0("$height", lastMouseX - 15, lastMouseY + 5, 0x3eeff)
                guiGraphics.drawString0("$rescaledHeight", lastMouseX - 15, lastMouseY + 15, 0x3eeff)
                guiGraphics.drawString0("$lineHeight", lastMouseX - 15, lastMouseY + 25, 0x3eeff)
                guiGraphics.drawString0("${getLinesPerPage()}", lastMouseX - 15, lastMouseY + 35, 0x3eeff)
                guiGraphics.drawString0("$rescaledLinesPerPage", lastMouseX - 15, lastMouseY + 45, 0x3eeff)
            }
        }
    }

    private fun handleScreenResize() {
        val screenWidth = Minecraft.getInstance().window.guiScaledWidth
        val screenHeight = Minecraft.getInstance().window.guiScaledHeight

        val widthChanged = screenWidth != previousScreenWidth && previousScreenWidth != -1
        val heightChanged = screenHeight != previousScreenHeight && previousScreenHeight != -1

        previousScreenWidth = screenWidth
        previousScreenHeight = screenHeight

        if (!widthChanged && !heightChanged) return

        // Reset internal dimensions to player-set values so updateCachedDimension re-clamps them
        if (widthChanged) internalWidth = width
        if (heightChanged) internalHeight = height

        // updateCachedDimension now calls computeAnchorInternalX/Y which use the anchor-relative
        // x/y fields and the current screen size — no separate anchor override needed here.
        updateCachedDimension()

        if (widthChanged) {
            chatWindow.tabSettings.tabs.forEach { it.rescaleChat() }
        }
    }

    /**
     * Converts the anchor-relative renderer.x to an absolute internalX for the current screen.
     * Called by updateCachedDimension.
     */
    private fun computeAnchorInternalX(): Int {
        val anchor = runCatching { chatWindow.generalSettings.anchorPoint }.getOrElse { AnchorPoint.BOTTOM_LEFT }
        val screenW = Minecraft.getInstance().window.guiScaledWidth
        val raw = anchor.anchorXToAbsolute(x, internalWidth, screenW)
        if (Config.values.allowWindowsOutsideScreen) {
            return raw
        }
        return raw.coerceIn(0, maxOf(0, screenW - internalWidth))
    }

    /**
     * Converts the anchor-relative renderer.y to an absolute internalY for the current screen.
     * Called by updateCachedDimension and the RenderValidateYEvent check.
     * Uses the event-driven getMinYScaled/getMaxYScaled so that tab bars (TOP/BOTTOM position)
     * are properly accounted for in the clamping range.
     */
    private fun computeAnchorInternalY(): Int {
        val anchor = runCatching { chatWindow.generalSettings.anchorPoint }.getOrElse { AnchorPoint.BOTTOM_LEFT }
        val screenH = Minecraft.getInstance().window.guiScaledHeight
        val raw = anchor.anchorYToAbsolute(y, internalHeight, screenH)
        if (Config.values.allowWindowsOutsideScreen) {
            return raw
        }
        val minY = getMinYScaled()
        val maxY = getMaxYScaled()
        return raw.coerceIn(minOf(minY, maxY), maxOf(minY, maxY))
    }

    fun getTimeFactor(ticksLived: Int): Double {
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
            w = if (Config.values.allowWindowsOutsideScreen && guiWidth - x <= 0) {
                MIN_WIDTH
            } else {
                MIN_WIDTH.coerceAtMost(guiWidth - x)
            }
            status = UpdateWidthStatus.LESS_THAN_ZERO
        }
        if (!Config.values.allowWindowsOutsideScreen && x + w >= guiWidth) {
            w = guiWidth - x
            status = UpdateWidthStatus.GREATER_THAN_GUI_WIDTH
        }
        return UpdateWidth(status, w)
    }

    fun getBackgroundWidth(): Float {
        return getUpdatedWidthValue() / getUpdatedScale()
    }

    fun getUpdatedHeight(heightType: HeightType): Int {
        return getUpdatedHeight(internalHeight, heightType)
    }

    fun getUpdatedHeight(startingHeight: Int, heightType: HeightType): Int {
        var h = EventBus.post(GetHeightEvent(chatWindow, startingHeight, heightType)).startingHeight
        val minHeight = getMinHeight()
        val lowerThanMin = h < minHeight
        val hasSpace = internalY - 1 >= minHeight
        if (lowerThanMin && hasSpace) {
            h = minHeight
        }
        // When allowWindowsOutsideScreen is on, do not auto-fit height to screen (so moving chat up keeps height)
        if (!Config.values.allowWindowsOutsideScreen) {
            val maxHeightScaled = getMaxHeightScaled()
            if (h > maxHeightScaled) {
                h = maxHeightScaled
            }
            if (h >= internalY) {
                h = maxHeightScaled
            }
        }
        return h
    }

    fun getUpdatedX(): Int {
        return getUpdatedX(internalX)
    }

    fun getUpdatedX(startingX: Int): Int {
        if (Config.values.allowWindowsOutsideScreen) {
            return startingX
        }
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
        return getUpdatedY(internalY)
    }

    fun getUpdatedY(startingY: Int): Int {
        var y = startingY
        if (y == -1) {
            y = Minecraft.getInstance().window.guiScaledHeight
        }
        if (Config.values.allowWindowsOutsideScreen) {
            return y
        }
        if (y < 0) {
            y += Minecraft.getInstance().window.guiScaledHeight
        }
        if (y >= Minecraft.getInstance().window.guiScaledHeight) {
            y = getMaxYScaled()
        }
        return y
    }

    fun getUpdatedLineHeight(): Int {
        return (9.0 * (chatWindow.generalSettings.lineSpacing + 1.0)).toInt()
    }

    fun getLinesPerPage(heightType: HeightType = HeightType.ADJUSTED): Int {
        return (getUpdatedHeight(heightType) / getUpdatedLineHeight().toDouble()).roundToInt()
    }

    fun getLinesPerPageScaled(heightType: HeightType = HeightType.ADJUSTED): Int {
        return max((getUpdatedHeight(heightType) / getUpdatedLineHeight().toDouble() / getUpdatedScale()).toInt(), 1)
    }

    fun getTotalLineHeight(wholeWindow: Boolean = Config.values.movableChatEnabled): Float {
        val lineCount = if (wholeWindow) {
            getLinesPerPageScaled(HeightType.ADJUSTED)
        } else {
            min(chatWindow.tabSettings.selectedTab.displayedMessages.size, getLinesPerPageScaled(HeightType.ADJUSTED))
        }
        val totalLineHeight = lineCount * lineHeight * scale
        return EventBus.post(GetTotalLineHeightEvent(chatWindow, totalLineHeight)).totalLineHeight
    }

    fun getMaxHeightScaled(heightType: HeightType = HeightType.RAW): Int {
        val maxHeight = EventBus.post(GetMaxHeightEvent(chatWindow, heightType, internalY)).maxHeight
        return getNormalizedHeight(maxHeight)
    }

    fun getMinYScaled(): Int {
        return EventBus.post(GetMinYEvent(chatWindow, getTotalLineHeight().roundToInt())).minY
    }

    fun getMaxYScaled(): Int {
        return EventBus.post(GetMaxYEvent(chatWindow, Minecraft.getInstance().window.guiScaledHeight)).maxY
    }

    fun getUpdatedScale(): Float {
        val scale = chatWindow.generalSettings.scale
        if (scale <= 0) {
            return .001f
        }
        return scale
    }

    fun getMinHeight(): Int {
        return EventBus.post(GetMinHeightEvent(chatWindow, MIN_HEIGHT)).minHeight
    }

    fun getNormalizedHeight(height: Int): Int {
        val scaledHeight = ceil(height / scale).toInt()
        return ceil((scaledHeight - (scaledHeight % lineHeight)) * scale).toInt()
    }

}

data class GetMinHeightEvent(val chatWindow: ChatWindow, var minHeight: Int)

data class RenderValidateYEvent(val renderer: ChatRenderer, var internalY: Int)
