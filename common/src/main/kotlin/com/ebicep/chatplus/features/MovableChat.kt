package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.fill0
import com.ebicep.chatplus.util.KeyUtil.isDown
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth
import kotlin.math.roundToInt

object MovableChat {

    private const val RENDER_MOVING_SIZE = 4f // width/length of box when rendering moving chat
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
        EventBus.register<ChatScreenMouseClickedEvent>({ 50 }, { movingChat }) {
            if (it.button != 0 || !Config.values.keyMoveChat.isDown()) {
                return@register
            }
            val chatWindow = ChatManager.selectedWindow
            val renderer = chatWindow.renderer
            val side = renderer.getUpdatedX() + renderer.getUpdatedWidth()
            val sideInner = renderer.getUpdatedX() + renderer.getUpdatedWidth() - RENDER_MOVING_SIZE
            val roof = renderer.getUpdatedY() - renderer.getUpdatedHeight()
            val roofInner = renderer.getUpdatedY() - renderer.getUpdatedHeight() + RENDER_MOVING_SIZE
            val mouseX = it.mouseX
            val mouseY = it.mouseY
            if (mouseX > sideInner && mouseX < side && mouseY > roof && mouseY < renderer.getUpdatedY()) {
                movingChatX = true
            }
            if (mouseY < roofInner && mouseY > roof && mouseX > renderer.getUpdatedX() && mouseX < side) {
                movingChatY = true
            }
            if (!movingChatX && !movingChatY) {
                if (
                    renderer.getUpdatedX() < mouseX && mouseX < sideInner &&
                    roofInner < mouseY && mouseY < renderer.getUpdatedY()
//                    || chatWindow.getClickedTab(mouseX, mouseY) != null
                ) {
                    movingChatBox = true
                    xDisplacement = mouseX - renderer.getUpdatedX()
                    yDisplacement = mouseY - renderer.getUpdatedY()
                }
            }
        }

        EventBus.register<ChatScreenMouseReleasedEvent> {
            if (movingChat) {
                movingChat = false
                it.returnFunction = true
            }
        }
        EventBus.register<ChatScreenMouseDraggedEvent>({ 50 }, { movingChat }) {
            if (!ChatManager.isChatFocused() || it.button != 0 || !Config.values.keyMoveChat.isDown()) {
                movingChat = false
                return@register
            }
            val chatWindow = ChatManager.selectedWindow
            val renderer = chatWindow.renderer
            val mouseX = it.mouseX
            val mouseY = it.mouseY
            if (movingChatX) {
                val newWidth: Double = Mth.clamp(
                    mouseX - renderer.getUpdatedX(),
                    MIN_WIDTH.toDouble(),
                    Minecraft.getInstance().window.guiScaledWidth - renderer.getUpdatedX().toDouble()
                )
                val width = newWidth.roundToInt()
                renderer.width = width
            }
            if (movingChatY) {
                val newHeight: Double = Mth.clamp(
                    renderer.getUpdatedY() - mouseY,
                    MIN_HEIGHT.toDouble(),
                    renderer.getUpdatedY() - 1.0
                )
                val height = newHeight.roundToInt()
                renderer.height = height
            }
            if (movingChatBox) {
                renderer.x = Mth.clamp(
                    (mouseX - xDisplacement).roundToInt(),
                    0,
                    Minecraft.getInstance().window.guiScaledWidth - renderer.getUpdatedWidth() - 1
                )
                val maxHeightScaled = renderer.getMaxHeightScaled()
                var newY = Mth.clamp(
                    (mouseY - yDisplacement).roundToInt(),
                    renderer.getUpdatedHeight() + 1,
                    maxHeightScaled
                )
                if (newY == maxHeightScaled) {
                    newY = renderer.getDefaultY()
                }
                renderer.y = newY
                renderer.updateCachedDimension()
            }
        }

        var moving = false
        EventBus.register<ChatRenderPreLinesEvent> {
            val chatWindow = it.chatWindow
            // for when there are no messages
            moving = ChatManager.isChatFocused() && Config.values.keyMoveChat.isDown()
            val messagesToDisplay = chatWindow.selectedTab.displayedMessages.size
            if (messagesToDisplay > 0) {
                return@register
            }
            val renderer = chatWindow.renderer
            // render full chat box
            val guiGraphics = it.guiGraphics
            if (moving) {
                guiGraphics.fill(
                    renderer.internalX,
                    renderer.internalY - renderer.internalHeight,
                    renderer.backgroundWidthEndX,
                    renderer.internalY,
                    chatWindow.backgroundColor
                )
            }
            renderMoving(
                guiGraphics.pose(),
                guiGraphics,
                renderer.internalX,
                renderer.internalY,
                renderer.internalHeight,
                renderer.internalWidth
            )
            it.returnFunction = true
        }
        EventBus.register<ChatRenderPostLinesEvent>({ 50 }, { movingChat }) {
            if (!moving) {
                return@register
            }
            val chatWindow = it.chatWindow
            val renderer = chatWindow.renderer
            val guiGraphics = it.guiGraphics
            guiGraphics.fill(
                renderer.rescaledX,
                renderer.rescaledY - renderer.rescaledHeight,
                renderer.rescaledEndX,
                renderer.rescaledY - it.displayMessageIndex * renderer.lineHeight,
                chatWindow.backgroundColor
            )
            if (it.chatWindow == ChatManager.selectedWindow) {
                val poseStack = guiGraphics.pose()
                poseStack.createPose {
                    val unscaled = 1 / renderer.scale
                    poseStack.scale(unscaled, unscaled, 1f)
                    renderMoving(
                        poseStack,
                        guiGraphics,
                        renderer.internalX,
                        renderer.internalY,
                        renderer.internalHeight,
                        renderer.internalWidth
                    )
                }
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
            if (movingChatX) {
                guiGraphics.fill0(
                    x + backgroundWidth - RENDER_MOVING_SIZE,
                    y - height.toFloat(),
                    x + backgroundWidth.toFloat() + .75f,
                    y.toFloat() + .5f,
                    200,
                    0xFFFFFFFF.toInt()
                )
            }
            if (movingChatY) {
                guiGraphics.fill0(
                    x.toFloat(),
                    y - height.toFloat() - .25f,
                    x + backgroundWidth.toFloat(),
                    y - height + RENDER_MOVING_SIZE,
                    200,
                    0xFFFFFFFF.toInt()
                )
            }
        }
    }


}