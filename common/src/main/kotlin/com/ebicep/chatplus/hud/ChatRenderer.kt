package com.ebicep.chatplus.hud

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.hud.ChatManager.getDefaultY
import com.ebicep.chatplus.hud.ChatManager.getMaxHeightScaled
import com.ebicep.chatplus.hud.ChatManager.selectedTab
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.GuiMessage
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth
import kotlin.math.ceil
import kotlin.math.roundToInt

abstract class ChatRenderLineEvent(
    open val guiGraphics: GuiGraphics,
    open val chatPlusGuiMessageLine: ChatTab.ChatPlusGuiMessageLine,
    open val verticalChatOffset: Int,
    open val verticalTextOffset: Int,
) : Event {
    val line: GuiMessage.Line
        get() = chatPlusGuiMessageLine.line
}

class ChatRenderLineBackgroundEvent(
    guiGraphics: GuiGraphics,
    chatPlusGuiMessageLine: ChatTab.ChatPlusGuiMessageLine,
    verticalChatOffset: Int,
    verticalTextOffset: Int,
    var backgroundColor: Int,
) : ChatRenderLineEvent(guiGraphics, chatPlusGuiMessageLine, verticalChatOffset, verticalTextOffset)

class ChatRenderLineTextEvent(
    guiGraphics: GuiGraphics,
    chatPlusGuiMessageLine: ChatTab.ChatPlusGuiMessageLine,
    verticalChatOffset: Int,
    verticalTextOffset: Int,
    val text: String,
) : ChatRenderLineEvent(guiGraphics, chatPlusGuiMessageLine, verticalChatOffset, verticalTextOffset)


data class ChatRenderPreLinesEvent(
    val guiGraphics: GuiGraphics,
    var returnFunction: Boolean = false
) : Event

data class ChatRenderPostLinesEvent(
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
    var rescaledEndX: Int = 0
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
        rescaledX = ceil(x / scale).toInt()
        rescaledY = ceil(y / scale).toInt()
        rescaledHeight = ceil(height / scale).toInt()
        rescaledWidth = ceil(width / scale).toInt()
        rescaledEndX = ceil(backgroundWidthEndX / scale).toInt()
        rescaledLinesPerPage = ChatManager.getLinesPerPageScaled()
        lineHeight = ChatManager.getLineHeight()
    }

    fun render(guiGraphics: GuiGraphics, guiTicks: Int, mouseX: Int, mouseY: Int) {
        handleScreenResize()

        val poseStack: PoseStack = guiGraphics.pose()
        val chatFocused: Boolean = ChatManager.isChatFocused()

        if (EventBus.post(ChatRenderPreLinesEvent(guiGraphics)).returnFunction) {
            return
        }

        val messagesToDisplay = selectedTab.displayedMessages.size
        poseStack.pushPose()
        poseStack.scale(scale, scale, 1.0f)
        var displayMessageIndex = 0
        var linesPerPage = rescaledLinesPerPage
        if (!chatFocused) {
            linesPerPage /= 2
        }
        while (displayMessageIndex + selectedTab.chatScrollbarPos < messagesToDisplay && displayMessageIndex < linesPerPage) {
            val messageIndex = messagesToDisplay - displayMessageIndex - selectedTab.chatScrollbarPos
            val chatPlusGuiMessageLine: ChatTab.ChatPlusGuiMessageLine = selectedTab.displayedMessages[messageIndex - 1]
            val line: GuiMessage.Line = chatPlusGuiMessageLine.line
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

            poseStack.createPose {
                poseStack.guiForward()
                val renderLineBackgroundEvent = ChatRenderLineBackgroundEvent(
                    guiGraphics,
                    chatPlusGuiMessageLine,
                    verticalChatOffset,
                    verticalTextOffset,
                    backgroundColor shl 24
                )
                EventBus.post(renderLineBackgroundEvent)
                //background
                guiGraphics.fill(
                    rescaledX,
                    verticalChatOffset - lineHeight,
                    rescaledEndX,
                    verticalChatOffset,
                    renderLineBackgroundEvent.backgroundColor
                )
                poseStack.guiForward()
                EventBus.post(
                    ChatRenderLineTextEvent(
                        guiGraphics,
                        chatPlusGuiMessageLine,
                        verticalChatOffset,
                        verticalTextOffset,
                        chatPlusGuiMessageLine.content
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
        if (EventBus.post(ChatRenderPostLinesEvent(guiGraphics, displayMessageIndex)).returnFunction) {
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
            Config.values.x = (screenWidth * Config.values.x / previousScreenWidth.toDouble()).roundToInt()
        }
        if (heightChanged) {
            val oldY = Config.values.y
            val defaultY = getDefaultY()
            if (oldY <= 0) {
                Config.values.y = defaultY
            } else {
                if (oldY >= getMaxHeightScaled(previousScreenHeight)) {
                    Config.values.y = getMaxHeightScaled()
                } else {
                    val oldRatio = oldY / previousScreenHeight.toDouble()
                    var newY = (screenHeight * oldRatio).roundToInt()
                    if (newY >= getMaxHeightScaled()) {
                        newY = defaultY
                    }
                    Config.values.y = newY
                }
            }
            val oldHeight = Config.values.height
            if ((oldY > 0 && oldHeight >= oldY - 1) ||
                (oldY == defaultY && oldHeight >= getMaxHeightScaled(previousScreenHeight) - 1)
            ) {
                Config.values.height = getMaxHeightScaled() - 1
            }
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

}