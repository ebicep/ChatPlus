package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.Debug.debug
import com.ebicep.chatplus.features.chattabs.ChatTab.Companion.TAB_HEIGHT
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.features.chatwindows.ChatWindows
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatPlusScreen.lastMouseX
import com.ebicep.chatplus.hud.ChatPlusScreen.lastMouseY
import com.ebicep.chatplus.hud.ChatScreenMouseDraggedEvent
import com.ebicep.chatplus.hud.ChatScreenMouseReleasedEvent
import com.ebicep.chatplus.hud.ChatScreenRenderEvent
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

object ChatTabsMover {

    private const val MOVE_PADDING_X = 4
    private const val MOVE_PADDING_Y = TAB_HEIGHT
    private var movingTab: Boolean = false
    private var movingTabMouseXStart: Int = 0
    private var movingTabMouseYStart: Int = 0
    private var movingTabXOffset: Int = 0
    private var movingTabXStart: Int = 0
    private var movingTabYOffset: Int = 0
    private var movingTabYStart: Int = 0
    private val innerTabXOffset: Int
        get() = movingTabMouseXStart - movingTabXStart
    private val innerTabYOffset: Int
        get() = movingTabMouseYStart - movingTabYStart

    init {
        EventBus.register<ChatScreenMouseDraggedEvent> {
            if (!movingTab) {
                return@register
            }
            val chatWindow = ChatManager.selectedWindow
            val selectedTab = ChatManager.globalSelectedTab
            val movingTabIndex: Int = chatWindow.tabs.indexOf(selectedTab)
            if (movingTabIndex == -1) {
                return@register
            }
            movingTabXOffset = (it.mouseX - movingTabMouseXStart).roundToInt()
            movingTabYOffset = (it.mouseY - movingTabMouseYStart).roundToInt()
            if (outsideTabBar(chatWindow, it.mouseX, it.mouseY) != RelativeMouseTabBarPosition.INSIDE) {
                // check if tab is moved to new window
                val windowMovedTo: ChatWindow? = getWindowMovedTo(chatWindow, it.mouseX, it.mouseY)
                if (windowMovedTo != null) {
                    ChatPlus.LOGGER.info("Moved tab to new window $windowMovedTo")
                    removeTabFromWindow(chatWindow, selectedTab)

                    val newStartX = windowMovedTo.tabs.last().xEnd + CHAT_TAB_X_SPACE
                    ChatPlus.LOGGER.info("movingTabMouseXStart: $movingTabMouseXStart")
                    val oldWidth = windowMovedTo.getTabBarWidth()

                    selectedTab.chatWindow = windowMovedTo
                    windowMovedTo.tabs.add(selectedTab)
                    windowMovedTo.selectedTabIndex = windowMovedTo.tabs.size - 1
                    ChatWindows.selectWindow(windowMovedTo)
                    ChatPlus.LOGGER.info("New selected tab index: ${windowMovedTo.selectedTabIndex} = ${ChatManager.globalSelectedTab.name}")

//                    movingTab = false
                    // make sure tab is viewed in same place but with offset based on new window
                    movingTabMouseXStart = windowMovedTo.renderer.internalX + oldWidth + CHAT_TAB_X_SPACE + innerTabXOffset
                    movingTabMouseYStart = windowMovedTo.renderer.internalY + innerTabYOffset + CHAT_TAB_Y_OFFSET
                    movingTabXStart = newStartX.roundToInt()
                    movingTabYStart = windowMovedTo.tabs.first().yStart.roundToInt()
                    movingTabXOffset = (it.mouseX - movingTabMouseXStart).roundToInt()
                    movingTabYOffset = (it.mouseY - movingTabMouseYStart).roundToInt()
                    ChatPlus.LOGGER.info("movingTabMouseXStart: $movingTabMouseXStart, movingTabMouseYStart: $movingTabMouseYStart")
                    ChatPlus.LOGGER.info("movingTabXStart: $movingTabXStart, movingTabYStart: $movingTabYStart")
                    ChatPlus.LOGGER.info("movingTabXOffset: $movingTabXOffset, movingTabYOffset: $movingTabYOffset")
                    selectedTab.xStart = newStartX
                    selectedTab.yStart = windowMovedTo.tabs.last().yStart
                    ChatPlus.LOGGER.info("xStart: ${selectedTab.xStart}, yStart: ${selectedTab.yStart}")
                } else if (chatWindow.tabs.size > 1) { // check if tab can become a new window
                    ChatPlus.LOGGER.info("Removed tab from $chatWindow to create new window")
                    removeTabFromWindow(chatWindow, selectedTab)

                    val newWindow = ChatWindow()
                    selectedTab.chatWindow = newWindow
                    newWindow.tabs = mutableListOf(selectedTab)

                    ChatPlus.LOGGER.info("movingTabXOffset: $movingTabXOffset movingTabYOffset: $movingTabYOffset")

                    val newRenderer = newWindow.renderer
                    newRenderer.x = (it.mouseX - innerTabXOffset).roundToInt()
                    newRenderer.y = (it.mouseY - innerTabYOffset - CHAT_TAB_Y_OFFSET).roundToInt()
                    newRenderer.width = chatWindow.renderer.width
                    newRenderer.height = chatWindow.renderer.height
                    newRenderer.updateCachedDimension()

                    Config.values.chatWindows.add(newWindow)

                    movingTabXStart = newRenderer.internalX
                    movingTabYStart = newRenderer.internalY + CHAT_TAB_Y_OFFSET
                    movingTabMouseXStart = it.mouseX.roundToInt()
                    movingTabMouseYStart = it.mouseY.roundToInt()
                    movingTabXOffset = 0
                    movingTabYOffset = 0
                }
            } else {
                // moving tabs within the same window
                for (otherTab in chatWindow.tabs) {
                    if (otherTab === selectedTab) {
                        continue
                    }
                    val tabIndex = chatWindow.tabs.indexOf(otherTab)
                    val movingLeft = tabIndex < movingTabIndex
                    val otherTabMiddleX = otherTab.xStart + otherTab.width / 2.0
                    val leftSwap = movingLeft && selectedTab.xStart < otherTabMiddleX
                    val rightSwap = !movingLeft && selectedTab.xEnd > otherTabMiddleX
                    if (leftSwap || rightSwap) {
                        chatWindow.tabs.add(tabIndex, chatWindow.tabs.removeAt(movingTabIndex))
                        chatWindow.selectedTabIndex = tabIndex
                        queueUpdateConfig = true
                        break
                    }
                }
            }
        }
        EventBus.register<ChatScreenMouseReleasedEvent> {
            if (movingTab) {
                movingTab = false
            }
        }
        EventBus.register<ChatTabClickedEvent> {
            movingTab = true
            movingTabMouseXStart = it.mouseX.roundToInt()
            movingTabMouseYStart = it.mouseY.roundToInt()
            movingTabXOffset = 0
            movingTabXStart = it.tabXStart.roundToInt()
            movingTabYOffset = 0
            movingTabYStart = it.tabYStart.roundToInt()
        }
        EventBus.register<ChatTabRenderEvent> {
            val guiGraphics = it.guiGraphics
            val poseStack = guiGraphics.pose()
            val moving = movingTab && it.chatTab === ChatManager.globalSelectedTab
            if (!moving) {
                return@register
            }
            val xOff = abs(movingTabXOffset)
            val yOff = abs(movingTabYOffset)
            val movingX = xOff > 4
            val movingY = yOff > 4
            val outsideTabBar = outsideTabBar(it.chatTab.chatWindow, lastMouseX.toDouble(), lastMouseY.toDouble(), 0, 0)
            if (outsideTabBar != RelativeMouseTabBarPosition.INSIDE || movingX || movingY) {
                it.xStart = (movingTabXStart + movingTabXOffset).toDouble()
                it.yStart = (movingTabYStart + movingTabYOffset).toDouble()
            }
            if (debug) {
                poseStack.createPose {
                    poseStack.guiForward()
                    poseStack.guiForward()
                    guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        "$outsideTabBar",
                        lastMouseX + 5,
                        lastMouseY + 45,
                        0xFF5050
                    )
                    guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        "$innerTabXOffset | $innerTabYOffset",
                        lastMouseX + 5,
                        lastMouseY + 55,
                        0xFF5050
                    )
                }
                poseStack.createPose {
                    poseStack.guiForward()
                    poseStack.guiForward()
                    poseStack.translate0(x = it.chatTab.xStart, y = it.chatTab.yStart)
                    guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        "${movingTabXOffset.toInt()}",
                        30,
                        -20,
                        0xFF5050
                    )
                    guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        "${movingTabYOffset.toInt()}",
                        30,
                        -10,
                        0xFF5050
                    )
                }
            }
        }
        EventBus.register<ChatScreenRenderEvent> {
            if (!movingTab || !debug) {
                return@register
            }
            val guiGraphics = it.guiGraphics
            val poseStack = guiGraphics.pose()
            poseStack.createPose {
                poseStack.translate0(z = 1000)
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    "${movingTabMouseXStart.toInt()}",
                    lastMouseX + 5,
                    lastMouseY + 25,
                    0xFF5050
                )
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    "${movingTabMouseYStart.toInt()}",
                    lastMouseX + 5,
                    lastMouseY + 35,
                    0xFF5050
                )
                // exit tab bar
                Config.values.chatWindows.forEach { window ->
                    val smallWidth = window === ChatManager.selectedWindow && window.tabs.size == 1
                    val width = if (smallWidth) window.getTabBarWidth() else window.renderer.rescaledEndX - window.renderer.internalX
                    guiGraphics.renderOutline(
                        window.renderer.internalX - MOVE_PADDING_X,
                        getTabStartY(window) - MOVE_PADDING_Y,
                        width + MOVE_PADDING_X * 2,
                        TAB_HEIGHT + MOVE_PADDING_Y * 2,
                        (0xFFFFFF00).toInt()
                    )
                }
                // enter tab bar
                Config.values.chatWindows.forEach { window ->
                    val selected = window === ChatManager.selectedWindow && window.tabs.size == 1
                    val width = if (selected) window.getTabBarWidth() else window.renderer.rescaledEndX - window.renderer.internalX
                    guiGraphics.renderOutline(
                        window.renderer.internalX,
                        getTabStartY(window),
                        width,
                        TAB_HEIGHT,
                        (0xFF00FF00).toInt()
                    )
                }
                // lines to offset
                // x line
                guiGraphics.fill(
                    movingTabMouseXStart.toInt(),
                    movingTabMouseYStart.toInt(),
                    (movingTabMouseXStart + movingTabXOffset).toInt(),
                    (movingTabMouseYStart + 1).toInt(),
                    (0xFFFF00FF).toInt()
                )
                // y line
                guiGraphics.fill(
                    (movingTabMouseXStart + movingTabXOffset).toInt(),
                    movingTabMouseYStart.toInt(),
                    (movingTabMouseXStart + movingTabXOffset + 1).toInt(),
                    (movingTabMouseYStart + movingTabYOffset).toInt(),
                    (0xFFFF00FF).toInt()
                )
            }
        }
    }

    private fun outsideTabBar(
        chatWindow: ChatWindow,
        mouseX: Double,
        mouseY: Double,
        paddingX: Int = MOVE_PADDING_X,
        paddingY: Int = MOVE_PADDING_Y
    ): RelativeMouseTabBarPosition {
        val renderer = chatWindow.renderer
        val barStartX = renderer.internalX - paddingX
        val barEndX = (if (chatWindow.tabs.size == 1) renderer.internalX + chatWindow.getTabBarWidth() else renderer.rescaledEndX) + paddingX
        val barStartY = getTabStartY(chatWindow) - paddingY
        val barEndY = getTabEndY(chatWindow) + paddingY
        when {
            mouseX < barStartX -> return RelativeMouseTabBarPosition.LEFT
            mouseX > barEndX -> return RelativeMouseTabBarPosition.RIGHT
            mouseY < barStartY -> return RelativeMouseTabBarPosition.TOP
            mouseY > barEndY -> return RelativeMouseTabBarPosition.BOTTOM
        }
        return RelativeMouseTabBarPosition.INSIDE
    }

    private fun getWindowMovedTo(
        chatWindow: ChatWindow,
        mouseX: Double,
        mouseY: Double
    ): ChatWindow? {
        Config.values.chatWindows
            .reversed()
            .filter { it !== chatWindow }
            .forEach { otherWindow ->
                val otherRenderer = otherWindow.renderer
                val insideX = otherRenderer.internalX < mouseX && mouseX < otherRenderer.rescaledEndX
                val insideY = getTabStartY(otherWindow) < mouseY && mouseY < getTabEndY(otherWindow)
                if (insideX && insideY) {
                    return otherWindow
                }
            }
        return null
    }

    private fun removeTabFromWindow(
        chatWindow: ChatWindow,
        selectedTab: ChatTab
    ) {
        chatWindow.tabs.remove(selectedTab)
        if (chatWindow.tabs.isEmpty()) {
            Config.values.chatWindows.remove(chatWindow)
        } else {
            chatWindow.selectedTabIndex = max(0, chatWindow.selectedTabIndex - 1)
            chatWindow.startRenderTabIndex = Mth.clamp(chatWindow.startRenderTabIndex, 0, chatWindow.tabs.size - 1)
        }
    }

    private fun getTabStartY(chatWindow: ChatWindow): Int {
        return chatWindow.renderer.internalY + CHAT_TAB_Y_OFFSET
    }

    private fun getTabEndY(chatWindow: ChatWindow): Int {
        return chatWindow.renderer.internalY + CHAT_TAB_Y_OFFSET + TAB_HEIGHT
    }

    enum class RelativeMouseTabBarPosition {

        INSIDE,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM,

    }

}