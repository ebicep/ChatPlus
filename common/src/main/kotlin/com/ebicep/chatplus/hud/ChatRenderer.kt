package com.ebicep.chatplus.hud

import com.ebicep.chatplus.config.ChatPlusKeyBindings
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.hud.ChatManager.selectedTab
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.GuiMessage
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth
import kotlin.math.roundToInt

object ChatRenderer {

    val tabYOffset = 1 // offset from text box
    val tabXBetween = 1 // space between categories
    val renderingMovingSize = 3 // width/length of box when rendering moving chat

    private var previousScreenWidth = -1
    private var previousScreenHeight = -1

    fun render(guiGraphics: GuiGraphics, guiTicks: Int, mouseX: Int, mouseY: Int) {
        val screenWidth = Minecraft.getInstance().window.guiScaledWidth
        val screenHeight = Minecraft.getInstance().window.guiScaledHeight

        // updating chat box to previous relative position
//        if (screenWidth != previousScreenWidth && previousScreenWidth != -1) {
//            Config.values.x = (screenWidth * Config.values.x / previousScreenWidth.toDouble()).roundToInt()
//        }
//        if (screenHeight != previousScreenHeight && previousScreenHeight != -1) {
//            val oldY = Config.values.y
//            if (oldY >= previousScreenHeight - baseYOffset) {
//                Config.values.y = screenHeight - baseYOffset
//            } else {
//                val oldRatio = oldY / previousScreenHeight.toDouble()
//                var newY = (screenHeight * oldRatio).roundToInt()
//                if (newY > screenHeight - baseYOffset) {
//                    newY = -baseYOffset
//                }
//                Config.values.y = newY
//            }
//            val oldHeight = Config.values.chatHeight
//            if (oldHeight >= oldY - 1) {
//                Config.values.chatHeight = Config.values.y - 1
//            } else {
//                Config.values.chatHeight =
//                    (screenHeight * Config.values.chatHeight / previousScreenHeight.toDouble()).roundToInt()
//            }
//        }
        previousScreenWidth = screenWidth
        previousScreenHeight = screenHeight

        val mc = Minecraft.getInstance()
        val poseStack = guiGraphics.pose()

        val chatFocused: Boolean = ChatManager.isChatFocused()
        val scale: Float = ChatManager.getScale()
        val x: Int = ChatManager.getX()
        val y: Int = ChatManager.getY()
        val height: Int = ChatManager.getHeight()
        val width: Int = ChatManager.getWidth()
        val backgroundWidthEndX = x + width

        val textOpacity: Double = ChatManager.getTextOpacity() * 0.9 + 0.1
        val backGroundOpacity: Float = ChatManager.getBackgroundOpacity()
        val lineSpacing: Float = ChatManager.getLineSpacing()
        val l1 = (-8.0 * (lineSpacing + 1.0) + 4.0 * lineSpacing).roundToInt()

        // tabs
        if (chatFocused) {
            renderTabs(poseStack, guiGraphics, x, y)
        }

        val moving = ChatManager.isChatFocused() && InputConstants.isKeyDown(mc.window.window, ChatPlusKeyBindings.MOVE_CHAT.key.value)
        val messagesToDisplay = selectedTab.displayedMessages.size
        if (messagesToDisplay <= 0) {
            // render full chat box
            if (moving) {
                guiGraphics.fill(
                    x,
                    y - height,
                    backgroundWidthEndX,
                    y,
                    (255 * backGroundOpacity).toInt() shl 24
                )
            }
            renderMoving(
                poseStack,
                guiGraphics,
                x,
                y,
                height,
                width
            )
            return
        }

        poseStack.pushPose()
        poseStack.scale(scale, scale, 1.0f)
        val rescaledX = (x / scale).toInt()
        val rescaledY = (y / scale).toInt()
        val rescaledHeight = (height / scale).toInt()
        val rescaledWidth = (backgroundWidthEndX / scale).toInt()
        val rescaledLinesPerPage: Int = ChatManager.getLinesPerPageScaled()
        val lineHeight: Int = ChatManager.getLineHeight()
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
            val backgroundColor = (255.0 * fadeOpacity * backGroundOpacity).toInt()
            if (textColor <= 3) {
                ++displayMessageIndex
                continue
            }
            // how high chat is from input bar, if changed need to change queue offset
            val verticalChatOffset = rescaledY - displayMessageIndex * lineHeight
            val verticalTextOffset = verticalChatOffset + l1 // align text with background

            poseStack.pushPose()
            poseStack.translate(0.0f, 0.0f, 50.0f)
            //background
            guiGraphics.fill(
                rescaledX,
                verticalChatOffset - lineHeight,
                rescaledWidth,
                verticalChatOffset,
                backgroundColor shl 24
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
            // copy outline
            ChatPlusScreen.lastCopiedMessage?.let {
                if (it.first != line) {
                    return@let
                }
                if (it.second < Events.currentTick) {
                    return@let
                }
                guiGraphics.renderOutline(
                    rescaledX,
                    verticalChatOffset - lineHeight,
                    (width / scale).toInt(),
                    lineHeight,
                    (0xd4d4d4FF).toInt()
                )
            }
            poseStack.popPose()

            ++displayMessageIndex
        }
        if (moving) {
            guiGraphics.fill(
                rescaledX,
                rescaledY - rescaledHeight,
                rescaledWidth,
                rescaledY - displayMessageIndex * lineHeight,
                (255 * backGroundOpacity).toInt() shl 24
            )
        }
        poseStack.popPose()


        poseStack.pushPose()
        if (moving) {
            renderMoving(
                poseStack,
                guiGraphics,
                x,
                y,
                height,
                width
            )
        }
        poseStack.popPose()
    }

    private fun renderTabs(
        poseStack: PoseStack,
        guiGraphics: GuiGraphics,
        x: Int,
        y: Int
    ) {
        poseStack.pushPose()
        poseStack.translate(x.toFloat(), y.toFloat() + tabYOffset, 0f)
        ChatManager.chatTabs.forEach {
            it.render(guiGraphics)
            poseStack.translate(
                Minecraft.getInstance().font.width(it.name).toFloat() + ChatTab.PADDING + ChatTab.PADDING + tabXBetween,
                0f,
                0f
            )
        }
        poseStack.popPose()
    }

    private fun renderMoving(
        poseStack: PoseStack,
        guiGraphics: GuiGraphics,
        x: Int,
        y: Int,
        height: Int,
        backgroundWidth: Int
    ) {
        poseStack.pushPose()
        poseStack.translate(0f, 0f, 200f)
        if (ChatPlusScreen.movingChatX) {
            guiGraphics.fill(
                x + backgroundWidth - renderingMovingSize,
                y - height,
                x + backgroundWidth,
                y,
                0xFFFFFFFF.toInt()
            )
        }
        if (ChatPlusScreen.movingChatY) {
            guiGraphics.fill(
                x,
                y - height,
                x + backgroundWidth,
                y - height + renderingMovingSize,
                0xFFFFFFFF.toInt()
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