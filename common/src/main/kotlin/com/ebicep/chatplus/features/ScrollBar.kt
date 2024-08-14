package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatTabAddDisplayMessageEvent
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.fill0
import kotlin.math.roundToInt

object ScrollBar {

    private const val MIN_HEIGHT = 10f
    private const val HALF_MIN_HEIGHT = MIN_HEIGHT / 2f
    private val barWidth: Int
        get() = Config.values.scrollbarWidth
    private var barStartX: Float = 0f
    private var barEndX: Float = 0f
    private var barBottomY: Float = 0f
    private var barTopY: Float = 0f
    private var scrolling: Boolean = false
    private var lastMouseY = 0.0
    private var lastScrollPos = 0
    private var scrollPerY = 0.0

    init {
        EventBus.register<ChatTabAddDisplayMessageEvent> {
            if (!Config.values.scrollbarEnabled) {
                return@register
            }
            it.maxWidth -= barWidth
        }
        EventBus.register<ChatRenderPostLinesEvent> {
            if (!Config.values.scrollbarEnabled) {
                return@register
            }
            if (!ChatManager.isChatFocused()) {
                return@register
            }
            val chatWindow = ChatManager.selectedWindow
            val renderer = chatWindow.renderer
            val messageCount = ChatManager.globalSelectedTab.displayedMessages.size
            val linesPerPage = renderer.rescaledLinesPerPage
            if (messageCount <= linesPerPage) {
                return@register
            }
            val guiGraphics = it.guiGraphics
            val poseStack = guiGraphics.pose()
            poseStack.createPose {
                val chatScrollbarPos = ChatManager.globalSelectedTab.chatScrollbarPos
                val lineHeight = renderer.lineHeight
                val displayHeight = linesPerPage * lineHeight
                barStartX = (renderer.rescaledEndX - barWidth).toFloat()
                barEndX = barStartX + barWidth
                barBottomY = -(chatScrollbarPos * displayHeight / messageCount.toFloat() - renderer.rescaledY)
                var barHeight = (displayHeight * displayHeight / (messageCount * lineHeight.toFloat()))
                barTopY = barBottomY - barHeight
                scrollPerY = -(displayHeight.toDouble() / messageCount - renderer.rescaledY) - renderer.rescaledY

                if (barHeight < HALF_MIN_HEIGHT) {
                    barHeight = MIN_HEIGHT
                    barBottomY += HALF_MIN_HEIGHT
                    barTopY = barBottomY - barHeight
                    barBottomY = barBottomY.coerceAtMost(renderer.rescaledY.toFloat())
                    barTopY = barTopY.coerceAtLeast((renderer.rescaledY - displayHeight).toFloat())
                }

                guiGraphics.fill0(
                    barStartX,
                    barBottomY,
                    barEndX,
                    barTopY,
                    200, // z
                    Config.values.scrollbarColor
                )
            }
        }
        EventBus.register<ChatScreenMouseClickedEvent>({ 25 }, { scrolling }) {
            if (!Config.values.scrollbarEnabled) {
                return@register
            }
            val chatWindow = ChatManager.selectedWindow
            val renderer = chatWindow.renderer
            val rescaledX: Double = it.mouseX / renderer.scale
            val rescaledY: Double = it.mouseY / renderer.scale
            val min: Double = minOf(barStartX, barEndX).toDouble()
            val max: Double = maxOf(barStartX, barEndX).toDouble()
            if (rescaledX in min..max &&
                barTopY <= rescaledY && rescaledY <= barBottomY
            ) {
                scrolling = true
                lastMouseY = rescaledY
                lastScrollPos = ChatManager.globalSelectedTab.chatScrollbarPos
            }
        }
        EventBus.register<ChatScreenMouseReleasedEvent> {
            scrolling = false
        }
        EventBus.register<ChatScreenCloseEvent> {
            scrolling = false
        }
        EventBus.register<ChatScreenMouseDraggedEvent> {
            if (scrolling) {
                val chatWindow = ChatManager.selectedWindow
                val renderer = chatWindow.renderer
                val yOffset = lastMouseY - it.mouseY / renderer.scale
                val scrollOffset = yOffset / scrollPerY
                val newScrollPos = lastScrollPos - scrollOffset
                ChatManager.globalSelectedTab.setScrollPos(newScrollPos.roundToInt())
            }
        }
    }

}