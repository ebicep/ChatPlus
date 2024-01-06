package com.ebicep.chatplus.hud

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatManager.selectedTab
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.GuiMessage
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth
import kotlin.math.roundToInt

const val tabYOffset = 1 // offset from text box
const val tabXBetween = 1 // space between categories
const val renderingMovingSize = 3 // width/length of box when rendering moving chat

data class RenderChatPreLinesEvent(
    val guiGraphics: GuiGraphics,
    var returnFunction: Boolean = false
) : Event

data class RenderChatLineEvent(
    val guiGraphics: GuiGraphics,
    val line: GuiMessage.Line,
    val verticalChatOffset: Int,
    val verticalTextOffset: Int,
) : Event

data class RenderChatPostLinesEvent(
    val guiGraphics: GuiGraphics,
    var displayMessageIndex: Int,
    var returnFunction: Boolean = false
) : Event


object ChatRenderer {

    private var previousScreenWidth = -1
    private var previousScreenHeight = -1

    // cached values since render is called every tick they only need to be calculated once/on change
    var textOpacity: Double = 0.0
    var backgroundOpacity: Float = 0f
    var lineSpacing: Float = 0f
    var l1 = 0
    var scale: Float = 0f
    var x: Int = 0
    var y: Int = 0
    var height: Int = 0
    var width: Int = 0
    var backgroundWidthEndX: Int = 0
    var rescaledX: Int = 0
    var rescaledY: Int = 0
    var rescaledHeight: Int = 0
    var rescaledWidth: Int = 0
    var rescaledLinesPerPage: Int = 0
    var lineHeight: Int = 0

    fun updateCachedDimension() {
        textOpacity = ChatManager.getTextOpacity() * 0.9 + 0.1
        backgroundOpacity = ChatManager.getBackgroundOpacity()
        lineSpacing = ChatManager.getLineSpacing()
        l1 = (-8.0 * (lineSpacing + 1.0) + 4.0 * lineSpacing).roundToInt()
        scale = ChatManager.getScale()
        x = ChatManager.getX()
        y = ChatManager.getY()
        height = ChatManager.getHeight()
        width = ChatManager.getWidth()
        backgroundWidthEndX = x + width
        rescaledX = (x / scale).toInt()
        rescaledY = (y / scale).toInt()
        rescaledHeight = (height / scale).toInt()
        rescaledWidth = (backgroundWidthEndX / scale).toInt()
        rescaledLinesPerPage = ChatManager.getLinesPerPageScaled()
        lineHeight = ChatManager.getLineHeight()
    }

    fun render(guiGraphics: GuiGraphics, guiTicks: Int, mouseX: Int, mouseY: Int) {
        handleScreenResize()

        val poseStack: PoseStack = guiGraphics.pose()
        val chatFocused: Boolean = ChatManager.isChatFocused()

        // tabs
        if (chatFocused) {
            renderTabs(poseStack, guiGraphics, x, y)
        }

        if (EventBus.post(RenderChatPreLinesEvent(guiGraphics)).returnFunction) {
            return
        }

        val messagesToDisplay = selectedTab.displayedMessages.size
        poseStack.pushPose()
        poseStack.scale(scale, scale, 1.0f)
        var displayMessageIndex = 0
        while (displayMessageIndex + selectedTab.chatScrollbarPos < messagesToDisplay && displayMessageIndex < rescaledLinesPerPage) {
            val messageIndex = messagesToDisplay - displayMessageIndex - selectedTab.chatScrollbarPos
            val line: GuiMessage.Line = selectedTab.displayedMessages[messageIndex - 1].line
            val ticksLived: Int = guiTicks - line.addedTime()
            if (ticksLived >= 200 && !chatFocused) {
                ++displayMessageIndex
                continue
            }
            val fadeOpacity = if (chatFocused) 1.0 else getTimeFactor(ticksLived)
            val textColor = (255.0 * fadeOpacity * textOpacity).toInt()
            val backgroundColor = (255.0 * fadeOpacity * backgroundOpacity).toInt()
            if (textColor <= 3) {
                ++displayMessageIndex
                continue
            }
            // how high chat is from input bar, if changed need to change queue offset
            val verticalChatOffset: Int = rescaledY - displayMessageIndex * lineHeight
            val verticalTextOffset: Int = verticalChatOffset + l1 // align text with background

            val hoveredOver = line === ChatPlusScreen.hoveredOverMessage

            poseStack.pushPose()
            poseStack.translate(0.0f, 0.0f, 50.0f)
            //background
            guiGraphics.fill(
                rescaledX,
                verticalChatOffset - lineHeight,
                rescaledWidth,
                verticalChatOffset,
                if (hoveredOver) Config.values.hoverHighlightColor else backgroundColor shl 24
            )
            poseStack.translate(0f, 0f, 50f)
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                line.content(),
                rescaledX,
                verticalTextOffset,
                16777215 + (textColor shl 24)
            )
            poseStack.translate(0f, 0f, 50f)

            EventBus.post(RenderChatLineEvent(guiGraphics, line, verticalChatOffset, verticalTextOffset))

            poseStack.popPose()

            ++displayMessageIndex
        }
        if (EventBus.post(RenderChatPostLinesEvent(guiGraphics, displayMessageIndex)).returnFunction) {
            return
        }
        poseStack.popPose()
    }

    private fun handleScreenResize() {
        val screenWidth = Minecraft.getInstance().window.guiScaledWidth
        val screenHeight = Minecraft.getInstance().window.guiScaledHeight

        if (screenWidth != previousScreenWidth && previousScreenWidth != -1) {
            Config.values.x = (screenWidth * Config.values.x / previousScreenWidth.toDouble()).roundToInt()
            updateCachedDimension()
        }
        if (screenHeight != previousScreenHeight && previousScreenHeight != -1) {
            val oldY = Config.values.y
            if (oldY <= 0) {
                Config.values.y = -BASE_Y_OFFSET
            } else {
                val oldRatio = oldY / previousScreenHeight.toDouble()
                var newY = (screenHeight * oldRatio).roundToInt()
                if (newY > screenHeight - BASE_Y_OFFSET) {
                    newY = -BASE_Y_OFFSET
                }
                Config.values.y = newY
            }
            val oldHeight = Config.values.height
            if ((oldY > 0 && oldHeight >= oldY - 1) ||
                (oldY == -BASE_Y_OFFSET && oldHeight >= previousScreenHeight - BASE_Y_OFFSET - 1)
            ) {
                Config.values.height = screenHeight - BASE_Y_OFFSET - 1
            }
            updateCachedDimension()
        }
        previousScreenWidth = screenWidth
        previousScreenHeight = screenHeight
    }

    private fun renderTabs(
        poseStack: PoseStack,
        guiGraphics: GuiGraphics,
        x: Int,
        y: Int
    ) {
        poseStack.pushPose()
        poseStack.translate(x.toFloat(), y.toFloat() + tabYOffset, 0f)
        Config.values.chatTabs.forEach {
            it.render(guiGraphics)
            poseStack.translate(
                Minecraft.getInstance().font.width(it.name).toFloat() + ChatTab.PADDING + ChatTab.PADDING + tabXBetween,
                0f,
                0f
            )
        }
        poseStack.popPose()
    }

    private fun getTimeFactor(ticksLived: Int): Double {
        var d0 = ticksLived.toDouble() / 200.0
        d0 = 1.0 - d0
        d0 *= 10.0
        d0 = Mth.clamp(d0, 0.0, 1.0)
        return d0 * d0
    }

}