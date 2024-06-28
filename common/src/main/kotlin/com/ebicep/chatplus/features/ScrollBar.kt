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
            val messageCount = ChatManager.selectedTab.displayedMessages.size
            val linesPerPage = ChatRenderer.rescaledLinesPerPage
            if (messageCount <= linesPerPage) {
                return@register
            }
            val guiGraphics = it.guiGraphics
            val poseStack = guiGraphics.pose()
            poseStack.createPose {
                val chatScrollbarPos = ChatManager.selectedTab.chatScrollbarPos
                val lineHeight = ChatRenderer.lineHeight
                val displayHeight = linesPerPage * lineHeight
                barStartX = (ChatRenderer.rescaledEndX - barWidth).toFloat()
                barEndX = barStartX + barWidth
                barBottomY = -(chatScrollbarPos * displayHeight / messageCount.toFloat() - ChatRenderer.rescaledY)
                var barHeight = (displayHeight * displayHeight / (messageCount * lineHeight.toFloat()))
                barTopY = barBottomY - barHeight
                scrollPerY = -(displayHeight.toDouble() / messageCount - ChatRenderer.rescaledY) - ChatRenderer.rescaledY

                if (barHeight < HALF_MIN_HEIGHT) {
                    barHeight = MIN_HEIGHT
                    barBottomY += HALF_MIN_HEIGHT
                    barTopY = barBottomY - barHeight
                    barBottomY = barBottomY.coerceAtMost(ChatRenderer.rescaledY.toFloat())
                    barTopY = barTopY.coerceAtLeast((ChatRenderer.rescaledY - displayHeight).toFloat())
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
            val rescaledX: Double = it.mouseX / ChatRenderer.scale
            val rescaledY: Double = it.mouseY / ChatRenderer.scale
            val min: Double = minOf(barStartX, barEndX).toDouble()
            val max: Double = maxOf(barStartX, barEndX).toDouble()
            if (rescaledX in min..max &&
                barTopY <= rescaledY && rescaledY <= barBottomY
            ) {
                scrolling = true
                lastMouseY = rescaledY
                lastScrollPos = ChatManager.selectedTab.chatScrollbarPos
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
                val yOffset = lastMouseY - it.mouseY / ChatRenderer.scale
                val scrollOffset = yOffset / scrollPerY
                val newScrollPos = lastScrollPos - scrollOffset
                ChatManager.selectedTab.setScrollPos(newScrollPos.roundToInt())
            }
        }
    }

}