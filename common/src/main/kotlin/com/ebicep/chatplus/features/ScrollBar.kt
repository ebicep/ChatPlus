package com.ebicep.chatplus.features

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatTabAddDisplayMessageEvent
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import java.awt.Color
import kotlin.math.roundToInt

object ScrollBar {

    private const val BAR_WIDTH = 6
    private var barStartX: Int = 0
    private var barEndX: Int = 0
    private var barBottomY: Int = 0
    private var barTopY: Int = 0
    private var scrolling: Boolean = false
    private var lastMouseY = 0.0
    private var lastScrollPos = 0
    private var scrollPerY = 0.0

    init {
        EventBus.register<ChatTabAddDisplayMessageEvent> {
            it.maxWidth -= BAR_WIDTH
        }
        EventBus.register<ChatRenderPostLinesEvent> {
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
                barStartX = ChatRenderer.rescaledEndX - BAR_WIDTH
                barEndX = barStartX + BAR_WIDTH
                barBottomY = -(chatScrollbarPos * displayHeight / messageCount - ChatRenderer.rescaledY)
                val barHeight = (displayHeight * displayHeight / (messageCount * lineHeight.toDouble())).roundToInt()
                barTopY = barBottomY - barHeight
                scrollPerY = -(displayHeight.toDouble() / messageCount - ChatRenderer.rescaledY) - ChatRenderer.rescaledY
                guiGraphics.fill(
                    barStartX,
                    barBottomY,
                    barEndX,
                    barTopY,
                    200, // z
                    Color(128, 134, 139, 255).rgb
                )
            }
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            val rescaledX: Double = it.mouseX / ChatRenderer.scale
            val rescaledY: Double = it.mouseY / ChatRenderer.scale
            if (barStartX <= rescaledX && rescaledX <= barEndX &&
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