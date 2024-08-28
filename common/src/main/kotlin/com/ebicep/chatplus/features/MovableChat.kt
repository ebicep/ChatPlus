package com.ebicep.chatplus.features

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.*
import com.ebicep.chatplus.features.chattabs.ChatTab.Companion.TAB_HEIGHT
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.features.chatwindows.ChatWindows
import com.ebicep.chatplus.features.internal.Debug.debug
import com.ebicep.chatplus.features.internal.OnScreenDisplayEvent
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.hud.ChatPlusScreen.EDIT_BOX_HEIGHT
import com.ebicep.chatplus.hud.ChatPlusScreen.lastMouseX
import com.ebicep.chatplus.hud.ChatPlusScreen.lastMouseY
import com.ebicep.chatplus.util.ComponentUtil.withColor
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.fill0
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import com.ebicep.chatplus.util.KeyUtil.isDown
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import java.awt.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

object MovableChat {

    val MOVABLE_CHAT_COLOR = Color(255, 255, 255).rgb
    private val MOVABLE_CHAT_ENABLED_COMPONENT = Component.literal("Movable Chat Enabled").withColor(MOVABLE_CHAT_COLOR)

    // moving chat box
    private const val RENDER_MOVING_SIZE = 5f // width/length of box when rendering moving chat
    private var movingChat: Boolean
        get() = movingChatWidth || movingChatHeight || movingChatBox
        set(value) {
            queueUpdateConfig = true
            movingChatWidth = value
            movingChatHeight = value
            movingChatBox = value
        }
    private var movingChatWidth = false
    private var movingChatHeight = false
    private var movingChatBox = false
    private var dragging = false

    // center of point to translate from
    private var xDisplacement = 0.0
    private var yDisplacement = 0.0

    // moving tabs
    private const val MOVE_PADDING_X = 2
    private const val MOVE_PADDING_Y = TAB_HEIGHT / 2
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
        var toggleCooldown = false
        EventBus.register<ChatScreenKeyPressedEvent>({ 2 }) {
            if (toggleCooldown) {
                return@register
            }
            if (Config.values.movableChatToggleKey.isDown()) {
                toggleCooldown = true
                Config.values.movableChatEnabled = !Config.values.movableChatEnabled
                ChatPlus.sendMessage(
                    Component.literal("Movable Chat ${if (Config.values.movableChatEnabled) "Enabled" else "Disabled"}")
                        .withStyle(if (Config.values.movableChatEnabled) ChatFormatting.GREEN else ChatFormatting.RED)
                )
                it.returnFunction = true
            }
        }
        EventBus.register<ChatScreenKeyReleasedEvent> {
            toggleCooldown = false
        }
        EventBus.register<ChatScreenCloseEvent> {
            movingChat = false
            movingTab = false
            dragging = false
        }
        EventBus.register<ChatScreenMouseReleasedEvent> {
            if (movingChat) {
                movingChat = false
                it.returnFunction = true
            }
            if (movingTab) {
                movingTab = false
            }
            dragging = false
        }
        EventBus.register<ChatScreenMouseClickedEvent>({ 50 }, { movingChat }) {
            if (it.button != 0 || !Config.values.movableChatEnabled) {
                return@register
            }
            val chatWindow = ChatManager.selectedWindow
            val renderer = chatWindow.renderer
            val mouseX = it.mouseX
            val mouseY = it.mouseY
            val insideChatBox = insideArea(
                mouseX,
                mouseY,
                renderer.getUpdatedX(),
                renderer.getUpdatedY() - renderer.getUpdatedHeight(),
                renderer.getUpdatedX() + renderer.getUpdatedWidthValue(),
                renderer.getUpdatedY()
            )
            if (insideChatBox) {
                val insideInnerChatBox = insideArea(
                    mouseX,
                    mouseY,
                    renderer.getUpdatedX() + RENDER_MOVING_SIZE,
                    renderer.getUpdatedY() - renderer.getUpdatedHeight() + RENDER_MOVING_SIZE,
                    renderer.getUpdatedX() + renderer.getUpdatedWidthValue() - RENDER_MOVING_SIZE,
                    renderer.getUpdatedY() - RENDER_MOVING_SIZE
                )
                if (insideInnerChatBox) {
                    movingChatBox = true
                    xDisplacement = mouseX - renderer.getUpdatedX()
                    yDisplacement = mouseY - renderer.getUpdatedY()
                } else {
                    if (mouseX > renderer.getUpdatedX() + renderer.getUpdatedWidthValue() - RENDER_MOVING_SIZE) {
                        movingChatWidth = true
                    }
                    if (mouseY < renderer.getUpdatedY() - renderer.getUpdatedHeight() + RENDER_MOVING_SIZE) {
                        movingChatHeight = true
                    }
                }
            }
            it.returnFunction = movingChat
        }
        EventBus.register<ChatTabClickedEvent> {
            movingTab = true
            if (isSingleTabWindow(it.chatTab.chatWindow)) {
                movingChatBox = true
                xDisplacement = it.mouseX - it.chatTab.chatWindow.renderer.getUpdatedX()
                yDisplacement = it.mouseY - it.chatTab.chatWindow.renderer.getUpdatedY()
            }
            movingTabMouseXStart = it.mouseX.roundToInt()
            movingTabMouseYStart = it.mouseY.roundToInt()
            movingTabXOffset = 0
            movingTabXStart = it.tabXStart.roundToInt()
            movingTabYOffset = 0
            movingTabYStart = it.tabYStart.roundToInt()
        }

        EventBus.register<ChatScreenMouseDraggedEvent>({ 50 }, { movingChat }) {
            if (!ChatManager.isChatFocused() || it.button != 0 || !Config.values.movableChatEnabled) {
                movingChat = false
                return@register
            }
            dragging = true
        }

        var moving = false
        EventBus.register<ChatRenderPreLinesEvent> {
            val chatWindow = it.chatWindow
            // for when there are no messages
            moving = ChatManager.isChatFocused() && Config.values.movableChatEnabled
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
                    chatWindow.getUpdatedBackgroundColor()
                )
                if (it.chatWindow == ChatManager.selectedWindow) {
                    renderMoving(
                        guiGraphics.pose(),
                        guiGraphics,
                        renderer.internalX,
                        renderer.internalY,
                        renderer.internalHeight,
                        renderer.internalWidth
                    )
                }
            }
            it.returnFunction = true
        }
        EventBus.register<ChatRenderPostLinesEvent>({ 50 }, { movingChat }) {
            if (!moving) {
                return@register
            }
            val chatWindow = it.chatWindow
            val renderer = chatWindow.renderer
            val guiGraphics = it.guiGraphics
            guiGraphics.fill0(
                renderer.rescaledX,
                renderer.rescaledY - renderer.getLinesPerPage() * renderer.lineHeight,
                renderer.rescaledEndX,
                renderer.rescaledY - it.displayMessageIndex * renderer.lineHeight,
                chatWindow.getUpdatedBackgroundColor()
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
        EventBus.register<ChatTabRenderEvent> {
            val guiGraphics = it.guiGraphics
            val chatTab = it.chatTab
            val isMovingTab = movingTab && chatTab === ChatManager.globalSelectedTab
            if (!isMovingTab) {
                return@register
            }
            val xOff = abs(movingTabXOffset)
            val yOff = abs(movingTabYOffset)
            val movingX = xOff > 4
            val movingY = yOff > 4
            val outsideTabBar = outsideTabBar(chatTab.chatWindow, lastMouseX.toDouble(), lastMouseY.toDouble(), 0, 0) != RelativeMouseTabBarPosition.INSIDE
            if (outsideTabBar || movingX || movingY) {
                it.xStart = movingTabXStart + movingTabXOffset
                it.yStart = movingTabYStart + movingTabYOffset
            }
            if (debug) {
                renderDebugTab(guiGraphics, chatTab, outsideTabBar)
            }
        }
        EventBus.register<ChatScreenRenderEvent> {
            val chatWindow = ChatManager.selectedWindow
            val renderer = chatWindow.renderer
            val mouseX = it.mouseX.toDouble()
            val mouseY = it.mouseY.toDouble()
            if (movingChatWidth) {
                val newWidth: Double = Mth.clamp(
                    mouseX - renderer.getUpdatedX(),
                    MIN_WIDTH.toDouble(),
                    Minecraft.getInstance().window.guiScaledWidth - renderer.getUpdatedX().toDouble()
                )
                val width = newWidth.roundToInt()
                renderer.width = width
            }
            if (movingChatHeight) {
                val newHeight: Double = Mth.clamp(
                    renderer.getUpdatedY() - mouseY,
                    MIN_HEIGHT.toDouble(),
                    renderer.getUpdatedY() - 1.0
                )
                val height = newHeight.roundToInt()
                val lineHeightScaled = renderer.lineHeight * renderer.scale
                renderer.height = (height - (height % lineHeightScaled) + lineHeightScaled).toInt()
            }
            if (movingChatBox && dragging) {
                renderer.x = Mth.clamp(
                    (mouseX - xDisplacement).roundToInt(),
                    0,
                    Minecraft.getInstance().window.guiScaledWidth - renderer.getUpdatedWidthValue()
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
            }
            if (movingChat) {
                renderer.updateCachedDimension()
            }
            if (!movingTab) {
                return@register
            }
            val selectedTab = ChatManager.globalSelectedTab
            val movingTabIndex: Int = chatWindow.tabs.indexOf(selectedTab)
            if (movingTabIndex == -1) {
                return@register
            }
            movingTabXOffset = (mouseX - movingTabMouseXStart).roundToInt()
            movingTabYOffset = (mouseY - movingTabMouseYStart).roundToInt()
            val outsideTabBar = outsideTabBar(chatWindow, mouseX, mouseY) != RelativeMouseTabBarPosition.INSIDE
            val singleTab = isSingleTabWindow(chatWindow)
            if (outsideTabBar || singleTab) {
                val windowMovedTo: ChatWindow? = getWindowMovedTo(chatWindow, mouseX, mouseY)
                if (windowMovedTo != null) { // check if tab is moved to new window
                    moveTabToWindow(selectedTab, windowMovedTo, mouseX, mouseY)
                } else if (!singleTab) { // check if tab can become a new window
                    createNewWindow(chatWindow, selectedTab, mouseX, mouseY)
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
            if (debug && movingTab) {
                renderDebugMoving(it.guiGraphics, chatWindow)
            }
        }
        EventBus.register<OnScreenDisplayEvent> {
            if (!Config.values.movableChatEnabled || !Config.values.movableChatShowEnabledOnScreen || !ChatManager.isChatFocused()) {
                return@register
            }
            it.components.add(MOVABLE_CHAT_ENABLED_COMPONENT)
        }
    }

    private fun createNewWindow(
        chatWindow: ChatWindow,
        selectedTab: ChatTab,
        mouseX: Double,
        mouseY: Double
    ) {
        ChatPlus.LOGGER.info("Removed tab from $chatWindow to create new window")
        removeTabFromWindow(chatWindow, selectedTab)

        val newWindow = chatWindow.clone()
        selectedTab.chatWindow = newWindow
        newWindow.tabs = mutableListOf(selectedTab)

        // creates new window with same x/y as window separated from
        val newRenderer = newWindow.renderer
        val newX = (mouseX - innerTabXOffset).roundToInt()
        val newY = (mouseY - innerTabYOffset - CHAT_TAB_Y_OFFSET).roundToInt()
        ChatPlus.LOGGER.info("New window at $newX, $newY")
        newRenderer.x = newRenderer.getUpdatedX(newX)
        newRenderer.y = newRenderer.getUpdatedY(newY)
        newRenderer.internalX = newRenderer.x
        newRenderer.internalY = newRenderer.y
        newRenderer.width = chatWindow.renderer.width
        newRenderer.height = chatWindow.renderer.height
        newRenderer.updateCachedDimension()

        Config.values.chatWindows.add(newWindow)

        movingChatBox = true
        var mX = mouseX // aligns tab with front/end of chat box when moving if window was created with mouse outside screen
        if (selectedTab.xStart < 0) {
            mX += innerTabXOffset
        } else if ((mX - innerTabXOffset + selectedTab.width) > Minecraft.getInstance().window.guiScaledWidth) {
            mX = Minecraft.getInstance().window.guiScaledWidth - 1 - selectedTab.width + innerTabXOffset.toDouble()
        }
        xDisplacement = mX - newRenderer.internalX
        yDisplacement = mouseY - newRenderer.internalY

        // realign tab to cursor
        movingTabXStart = newX
        movingTabYStart = newY + CHAT_TAB_Y_OFFSET
        movingTabMouseXStart = mouseX.roundToInt()
        movingTabMouseYStart = mouseY.roundToInt()
        movingTabXOffset = (mouseX - movingTabMouseXStart).roundToInt()
        movingTabYOffset = (mouseY - movingTabMouseYStart).roundToInt()
    }

    private fun moveTabToWindow(
        selectedTab: ChatTab,
        windowMovedTo: ChatWindow,
        mouseX: Double,
        mouseY: Double
    ) {
        removeTabFromWindow(ChatManager.selectedWindow, selectedTab)

        val newStartX = windowMovedTo.tabs.last().xEnd + CHAT_TAB_X_SPACE
        val oldWidth = windowMovedTo.getTabBarWidth()

        selectedTab.chatWindow = windowMovedTo
        selectedTab.rescaleChat()
        windowMovedTo.tabs.add(selectedTab)
        windowMovedTo.selectedTabIndex = windowMovedTo.tabs.size - 1
        ChatWindows.selectWindow(windowMovedTo)

        // make sure tab is viewed in same place but with offset based on new window
        movingTabMouseXStart = windowMovedTo.renderer.internalX + oldWidth + CHAT_TAB_X_SPACE + innerTabXOffset
        movingTabMouseYStart = windowMovedTo.renderer.internalY + innerTabYOffset + CHAT_TAB_Y_OFFSET
        movingTabXStart = newStartX
        movingTabYStart = windowMovedTo.tabs.first().yStart
        movingTabXOffset = (mouseX - movingTabMouseXStart).roundToInt()
        movingTabYOffset = (mouseY - movingTabMouseYStart).roundToInt()
        selectedTab.xStart = newStartX
        selectedTab.yStart = windowMovedTo.tabs.last().yStart
        movingChatBox = false
    }

    private fun renderDebugTab(
        guiGraphics: GuiGraphics,
        chatTab: ChatTab,
        outsideTabBar: Boolean
    ) {
        val poseStack = guiGraphics.pose()
        poseStack.createPose {
            // below cursor - if tab is outside tab bar
            poseStack.guiForward(amount = 100.0)
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
            // below cursor - offset from tab start position
            poseStack.translate0(x = chatTab.xStart, y = chatTab.yStart, z = 100)
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                "$movingTabXOffset",
                30,
                -20,
                0xFF5050
            )
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                "$movingTabYOffset",
                30,
                -10,
                0xFF5050
            )
        }
    }

    private fun renderDebugMoving(
        guiGraphics: GuiGraphics,
        chatWindow: ChatWindow
    ) {
        val poseStack = guiGraphics.pose()
        val renderer = chatWindow.renderer
        poseStack.createPose {
            poseStack.translate0(z = 1000)
            // tab mouse start position (bottom of cursor)
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                "$movingTabMouseXStart",
                lastMouseX + 5,
                lastMouseY + 25,
                0xFF5050
            )
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                "$movingTabMouseYStart",
                lastMouseX + 5,
                lastMouseY + 35,
                0xFF5050
            )
            // exit tab bar
            Config.values.chatWindows.forEach { window ->
                val smallWidth = window === ChatManager.selectedWindow && isSingleTabWindow(window)
                val width = if (smallWidth) window.getTabBarWidth() else window.renderer.rescaledEndX - window.renderer.internalX
                guiGraphics.renderOutline(
                    window.renderer.internalX - MOVE_PADDING_X,
                    getTabStartY(window) - MOVE_PADDING_Y,
                    width.toInt() + MOVE_PADDING_X * 2,
                    TAB_HEIGHT + MOVE_PADDING_Y * 2,
                    (0xFFFFFF00).toInt()
                )
            }
            // enter tab bar
            Config.values.chatWindows.forEach { window ->
                val selected = window === ChatManager.selectedWindow && isSingleTabWindow(window)
                val width = if (selected) window.getTabBarWidth() else window.renderer.rescaledEndX - window.renderer.internalX
                guiGraphics.renderOutline(
                    window.renderer.internalX,
                    getTabStartY(window),
                    width.toInt(),
                    TAB_HEIGHT,
                    (0xFF00FF00).toInt()
                )
            }
            // lines to offset
            // x line
            guiGraphics.fill(
                movingTabMouseXStart,
                movingTabMouseYStart,
                (movingTabMouseXStart + movingTabXOffset),
                (movingTabMouseYStart + 1),
                (0xFFFF00FF).toInt()
            )
            // y line
            guiGraphics.fill(
                (movingTabMouseXStart + movingTabXOffset),
                movingTabMouseYStart,
                (movingTabMouseXStart + movingTabXOffset + 1),
                (movingTabMouseYStart + movingTabYOffset),
                (0xFFFF00FF).toInt()
            )
            // x displacement line
            if (chatWindow == ChatManager.selectedWindow) {
                guiGraphics.fill(
                    (renderer.internalX + xDisplacement).toInt(),
                    renderer.internalY - renderer.internalHeight,
                    (renderer.internalX + xDisplacement + 1).toInt(),
                    renderer.internalY,
                    (0xFF00FFFF).toInt()
                )
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
            if (movingChatWidth) {
                guiGraphics.fill0(
                    x + backgroundWidth - RENDER_MOVING_SIZE,
                    y - height.toFloat(),
                    x + backgroundWidth.toFloat(),
                    y.toFloat(),
                    200,
                    0xFFFFFFFF.toInt()
                )
            }
            if (movingChatHeight) {
                guiGraphics.fill0(
                    x.toFloat(),
                    y - height.toFloat(),
                    x + backgroundWidth.toFloat(),
                    y - height + RENDER_MOVING_SIZE,
                    200,
                    0xFFFFFFFF.toInt()
                )
            }
        }
    }

    private fun insideArea(x: Double, y: Double, x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        return insideArea(x, y, x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
    }

    private fun insideArea(x: Double, y: Double, x1: Int, y1: Int, x2: Int, y2: Int): Boolean {
        return insideArea(x, y, x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
    }

    private fun insideArea(x: Double, y: Double, x1: Double, y1: Double, x2: Double, y2: Double): Boolean {
        if (x1 > x2) {
            return insideArea(x, y, x2, y2, x1, y1)
        }
        if (y1 > y2) {
            return insideArea(x, y, x1, y2, x2, y1)
        }
        return x in x1..x2 && y in y1..y2
    }

    private fun outsideTabBar(
        chatWindow: ChatWindow,
        mouseX: Double,
        mouseY: Double,
        paddingX: Int = MOVE_PADDING_X,
        paddingY: Int = MOVE_PADDING_Y
    ): RelativeMouseTabBarPosition {
        if (mouseY > Minecraft.getInstance().window.guiScaledHeight - EDIT_BOX_HEIGHT) {
            return RelativeMouseTabBarPosition.OUTSIDE_SCREEN
        }
        val renderer = chatWindow.renderer
        val barStartX = renderer.internalX - paddingX
        val barEndX = (if (isSingleTabWindow(chatWindow)) renderer.internalX + chatWindow.getTabBarWidth() else renderer.rescaledEndX).toFloat() + paddingX
        val barStartY = getTabStartY(chatWindow) - paddingY
        val barEndY = getTabEndY(chatWindow) + paddingY
        when {
            mouseX < barStartX -> return RelativeMouseTabBarPosition.OUTSIDE_LEFT
            mouseX > barEndX -> return RelativeMouseTabBarPosition.OUTSIDE_RIGHT
            mouseY < barStartY -> return RelativeMouseTabBarPosition.OUTSIDE_TOP
            mouseY > barEndY -> return RelativeMouseTabBarPosition.OUTSIDE_BOTTOM
        }
        return RelativeMouseTabBarPosition.INSIDE
    }

    private fun isSingleTabWindow(chatWindow: ChatWindow) = chatWindow.tabs.size == 1

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
        OUTSIDE_SCREEN,
        OUTSIDE_LEFT,
        OUTSIDE_RIGHT,
        OUTSIDE_TOP,
        OUTSIDE_BOTTOM,

    }


}