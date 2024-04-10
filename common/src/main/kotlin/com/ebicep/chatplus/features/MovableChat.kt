package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import com.ebicep.chatplus.util.KeyUtil.isDown
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth
import kotlin.math.roundToInt

object MovableChat {

    private const val RENDER_MOVING_SIZE = 3 // width/length of box when rendering moving chat
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
            val sideInner = ChatManager.getX() + ChatManager.getWidth() - RENDER_MOVING_SIZE
            val roof = ChatManager.getY() - ChatManager.getHeight()
            val roofInner = ChatManager.getY() - ChatManager.getHeight() + RENDER_MOVING_SIZE
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
                    MIN_WIDTH.toDouble(),
                    Minecraft.getInstance().window.guiScaledWidth - ChatManager.getX().toDouble()
                )
                val width = newWidth.roundToInt()
                Config.values.width = width
            }
            if (movingChatY) {
                val newHeight: Double = Mth.clamp(
                    ChatManager.getY() - mouseY,
                    MIN_HEIGHT.toDouble(),
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
                val maxHeightScaled = ChatManager.getMaxHeightScaled()
                var newY = Mth.clamp(
                    (mouseY - yDisplacement).roundToInt(),
                    ChatManager.getHeight() + 1,
                    maxHeightScaled
                )
                if (newY == maxHeightScaled) {
                    newY = ChatManager.getDefaultY()
                }
                Config.values.y = newY
                ChatRenderer.updateCachedDimension()
            }
        }

        var moving = false
        EventBus.register<ChatRenderPreLinesEvent> {
            // for when there are no messages
            moving = ChatManager.isChatFocused() && Config.values.keyMoveChat.isDown()
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
                ChatRenderer.rescaledEndX,
                ChatRenderer.rescaledY - it.displayMessageIndex * ChatRenderer.lineHeight,
                (255 * ChatRenderer.backgroundOpacity).toInt() shl 24
            )
            val poseStack = guiGraphics.pose()
            poseStack.createPose {
                val unscaled = 1 / ChatRenderer.scale
                poseStack.scale(unscaled, unscaled, 1f)
                renderMoving(
                    poseStack,
                    guiGraphics,
                    ChatRenderer.x,
                    ChatRenderer.y,
                    ChatRenderer.height,
                    ChatRenderer.width
                )
            }
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
            poseStack.translate0(z = 200f)
            if (movingChatX) {
                guiGraphics.fill(
                    x + backgroundWidth - RENDER_MOVING_SIZE,
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
                    y - height + RENDER_MOVING_SIZE,
                    0xFFFFFFFF.toInt()
                )
            }
        }
    }


}