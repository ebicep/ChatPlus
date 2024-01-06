package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth
import kotlin.math.roundToInt

object MovableChat {

    private var movingChat: Boolean
        get() = movingChatX || movingChatY || movingChatBox
        set(value) {
            queueUpdateConfig = true
            movingChatX = value
            movingChatY = value
            movingChatBox = value
        }
    private var movingChatX = false
    private var movingChatY = false
    private var movingChatBox = false
    private var xDisplacement = 0.0
    private var yDisplacement = 0.0

    init {
        EventBus.register<ChatScreenMouseClickedEvent> {
            if (it.button != 0) {
                return@register
            }
            val side = ChatManager.getX() + ChatManager.getWidth()
            val sideInner = ChatManager.getX() + ChatManager.getWidth() - renderingMovingSize
            val roof = ChatManager.getY() - ChatManager.getHeight()
            val roofInner = ChatManager.getY() - ChatManager.getHeight() + renderingMovingSize
            if (findEnabled) {
                ChatManager.selectedTab.getMessageAt(ChatPlusScreen.lastMouseX.toDouble(), ChatPlusScreen.lastMouseY.toDouble())
                    ?.let { message ->
                        val lineOffset = ChatManager.getLinesPerPage() / 3
                        val scrollTo = ChatManager.selectedTab.messages.size - message.linkedMessageIndex - lineOffset
                        findEnabled = false
                        ChatManager.selectedTab.refreshDisplayedMessage()
                        it.screen.rebuildWidgets0()
                        ChatManager.selectedTab.scrollChat(scrollTo)
                    }
            }
            val mouseX = it.mouseX
            val mouseY = it.mouseY
            if (mouseX > sideInner && mouseX < side && mouseY > roof && mouseY < ChatManager.getY()) {
                movingChatX = true
            }
            if (mouseY < roofInner && mouseY > roof && mouseX > ChatManager.getX() && mouseX < side) {
                movingChatY = true
            }
            val window = Minecraft.getInstance().window.window
            if (!movingChatX && !movingChatY && InputConstants.isKeyDown(window, Config.values.keyMoveChat.value)) {
                if (
                    mouseX > ChatManager.getX() && mouseX < sideInner &&
                    mouseY > roofInner && mouseY < ChatManager.getY()
                ) {
                    movingChatBox = true
                    xDisplacement = mouseX - ChatManager.getX()
                    yDisplacement = mouseY - ChatManager.getY()
                }
            }
        }

        EventBus.register<ChatScreenMouseReleasedEvent> {
            if (movingChat) {
                movingChat = false
                it.returnFunction = true
            }
        }

        EventBus.register<ChatScreenMouseDraggedEvent> {
            if (!ChatManager.isChatFocused() || it.button != 0) {
                movingChat = false
                return@register
            }

            val mouseX = it.mouseX
            val mouseY = it.mouseY
            if (movingChatX) {
                val newWidth: Double = Mth.clamp(
                    mouseX - ChatManager.getX(),
                    ChatManager.getMinWidthScaled().toDouble(),
                    Minecraft.getInstance().window.guiScaledWidth - ChatManager.getX() - 1.0
                )
                val width = newWidth.roundToInt()
                Config.values.width = width
            }
            if (movingChatY) {
                val newHeight: Double = Mth.clamp(
                    ChatManager.getY() - mouseY,
                    ChatManager.getMinHeightScaled().toDouble(),
                    ChatManager.getY() - 1.0
                )
                val height = newHeight.roundToInt()
                Config.values.height = height
            }
            if (movingChatBox) {
                Config.values.x = Mth.clamp(
                    (mouseX - xDisplacement).roundToInt(),
                    0,
                    Minecraft.getInstance().window.guiScaledWidth - ChatManager.getWidth() - 1
                )
                var newY = Mth.clamp(
                    (mouseY - yDisplacement).roundToInt(),
                    ChatManager.getHeight() + 1,
                    Minecraft.getInstance().window.guiScaledHeight - BASE_Y_OFFSET
                )
                if (newY == Minecraft.getInstance().window.guiScaledHeight - BASE_Y_OFFSET) {
                    newY = -BASE_Y_OFFSET
                }
                Config.values.y = newY
            }
        }

        var moving = false
        EventBus.register<ChatRenderPreLinesEvent> {
            moving = ChatManager.isChatFocused() && InputConstants.isKeyDown(
                Minecraft.getInstance().window.window,
                Config.values.keyMoveChat.value
            )
            val messagesToDisplay = ChatManager.selectedTab.displayedMessages.size
            if (messagesToDisplay > 0) {
                return@register
            }
            // render full chat box
            val guiGraphics = it.guiGraphics
            if (moving) {
                guiGraphics.fill(
                    ChatRenderer.x,
                    ChatRenderer.y - ChatRenderer.height,
                    ChatRenderer.backgroundWidthEndX,
                    ChatRenderer.y,
                    (255 * ChatRenderer.backgroundOpacity).toInt() shl 24
                )
            }
            renderMoving(
                guiGraphics.pose(),
                guiGraphics,
                ChatRenderer.x,
                ChatRenderer.y,
                ChatRenderer.height,
                ChatRenderer.width
            )
            it.returnFunction = true
        }
        EventBus.register<ChatRenderPostLinesEvent> {
            if (!moving) {
                return@register
            }
            val guiGraphics = it.guiGraphics
            guiGraphics.fill(
                ChatRenderer.rescaledX,
                ChatRenderer.rescaledY - ChatRenderer.rescaledHeight,
                ChatRenderer.rescaledWidth,
                ChatRenderer.rescaledY - it.displayMessageIndex * ChatRenderer.lineHeight,
                (255 * ChatRenderer.backgroundOpacity).toInt() shl 24
            )
            renderMoving(
                guiGraphics.pose(),
                guiGraphics,
                ChatRenderer.rescaledX,
                ChatRenderer.rescaledY,
                ChatRenderer.rescaledHeight,
                ChatRenderer.rescaledWidth
            )
        }

        EventBus.register<HoverHighlight.HoverHighlightRenderEvent> {
            if (movingChat) {
                it.cancelled = true
            }
        }
    }

    private fun renderMoving(
        poseStack: PoseStack,
        guiGraphics: GuiGraphics,
        x: Int,
        y: Int,
        height: Int,
        backgroundWidth: Int
    ) {
        poseStack.createPose {
            poseStack.translate(0f, 0f, 200f)
            if (movingChatX) {
                guiGraphics.fill(
                    x + backgroundWidth - renderingMovingSize,
                    y - height,
                    x + backgroundWidth,
                    y,
                    0xFFFFFFFF.toInt()
                )
            }
            if (movingChatY) {
                guiGraphics.fill(
                    x,
                    y - height,
                    x + backgroundWidth,
                    y - height + renderingMovingSize,
                    0xFFFFFFFF.toInt()
                )
            }
        }
    }


}