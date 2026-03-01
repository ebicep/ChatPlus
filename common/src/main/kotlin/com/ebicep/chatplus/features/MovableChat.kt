package com.ebicep.chatplus.features

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.MessageDirection
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.MovableChat.InputBoxSettings.Companion.INPUT_BOX_PADDING
import com.ebicep.chatplus.features.MovableChat.InputBoxSettings.Companion.PADDED_INPUT_BOX_HEIGHT
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_X_SPACE
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_Y_OFFSET
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.ChatTab.Companion.TAB_HEIGHT
import com.ebicep.chatplus.features.chattabs.ChatTabGetMessageAtEvent
import com.ebicep.chatplus.features.chatwindows.ChatTabClickedEvent
import com.ebicep.chatplus.features.chatwindows.ChatTabRenderEvent
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.features.chatwindows.ChatWindowsManager
import com.ebicep.chatplus.features.chatwindows.TabSettings.Position.BOTTOM
import com.ebicep.chatplus.features.chatwindows.TabSettings.Position.TOP
import com.ebicep.chatplus.features.internal.Debug.debug
import com.ebicep.chatplus.features.internal.OnScreenDisplayEvent
import com.ebicep.chatplus.features.textbarelements.AddTextBarElementEvent
import com.ebicep.chatplus.features.textbarelements.MovableChatToggleTextBarElement
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.hud.ChatManager.resetGlobalSortedTabs
import com.ebicep.chatplus.hud.ChatPlusScreen.EDIT_BOX_HEIGHT
import com.ebicep.chatplus.hud.ChatPlusScreen.lastMouseX
import com.ebicep.chatplus.hud.ChatPlusScreen.lastMouseY
import com.ebicep.chatplus.mixin.IMixinScreen
import com.ebicep.chatplus.util.ComponentUtil.withColor
import com.ebicep.chatplus.util.GraphicsUtil
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.drawString0
import com.ebicep.chatplus.util.GraphicsUtil.fill0
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import com.mojang.blaze3d.vertex.PoseStack
import kotlinx.serialization.Serializable
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
    val MIN_WIDTH_INPUT_BOX = 225.0

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

    // moving input box
    private var movingInputBox: Boolean
        get() = movingInputBoxPosition || movingInputBoxWidth
        set(value) {
            queueUpdateConfig = true
            movingInputBoxPosition = value
            movingInputBoxWidth = value
        }
    private var movingInputBoxPosition = false
    private var movingInputBoxWidth = false

    init {
        EventBus.register<ChatScreenInputEvent>({ 5 }) {
            if (it.checkRelease(Config.values.movableChatKey)) {
                return@register
            }
            Config.values.movableChatEnabled = !Config.values.movableChatEnabled
            ChatPlus.sendMessage(
                Component.literal("Movable Chat ${if (Config.values.movableChatEnabled) "Enabled" else "Disabled"}")
                    .withStyle(if (Config.values.movableChatEnabled) ChatFormatting.GREEN else ChatFormatting.RED)
            )
            it.returnFunction = true
        }
        EventBus.register<AddTextBarElementEvent>({ 75 }) {
            if (Config.values.movableChatToggleTextBarElement) {
                it.elements.add(MovableChatToggleTextBarElement(it.screen))
            }
        }
        EventBus.register<ChatTabGetMessageAtEvent> {
            if (movingChat) {
                it.returnFunction = true
            }
        }
        EventBus.register<ChatScreenCloseEvent> {
            movingChat = false
            movingTab = false
            movingInputBox = false
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
            if (movingInputBox) {
                movingInputBox = false
            }
            dragging = false
        }
        EventBus.register<ChatScreenMouseClickedEvent>({ 50 }, { movingChat || movingInputBoxWidth }) {
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
                renderer.getUpdatedY() - renderer.getTotalLineHeight().roundToInt(),
                renderer.getUpdatedX() + renderer.getUpdatedWidthValue(),
                renderer.getUpdatedY()
            )
            if (insideChatBox) {
                val insideInnerChatBox = insideArea(
                    mouseX,
                    mouseY,
                    renderer.getUpdatedX() + RENDER_MOVING_SIZE,
                    renderer.getUpdatedY() - renderer.getTotalLineHeight() + RENDER_MOVING_SIZE,
                    renderer.getUpdatedX() + renderer.getUpdatedWidthValue() - RENDER_MOVING_SIZE,
                    renderer.getUpdatedY() - RENDER_MOVING_SIZE
                )
                if (insideInnerChatBox) {
                    xDisplacement = mouseX - renderer.getUpdatedX()
                    yDisplacement = mouseY - renderer.getUpdatedY()
                    movingChatBox = true
                } else {
                    if (mouseX > renderer.getUpdatedX() + renderer.getUpdatedWidthValue() - RENDER_MOVING_SIZE) {
                        xDisplacement = renderer.getUpdatedX() + renderer.getUpdatedWidthValue() - mouseX
                        movingChatWidth = true
                    }
                    if (mouseY < renderer.getUpdatedY() - renderer.getTotalLineHeight() + RENDER_MOVING_SIZE) {
                        yDisplacement = renderer.getUpdatedY() - renderer.getTotalLineHeight() - mouseY
                        movingChatHeight = true
                    }
                }
            } else if (!Config.values.vanillaInputBox) {
                // check moving input box
                val inputBoxSettings = Config.values.inputBoxSettings
                val startX = inputBoxSettings.startX
                val startY = inputBoxSettings.getCalculatedStartY()
                if (insideArea(
                        mouseX,
                        mouseY,
                        startX - 2,
                        startY - INPUT_BOX_PADDING,
                        startX + inputBoxSettings.getCalculatedWidth(),
                        startY + PADDED_INPUT_BOX_HEIGHT
                    )
                ) {
                    if (mouseX > startX + inputBoxSettings.getCalculatedWidth() - RENDER_MOVING_SIZE) {
                        xDisplacement = startX + inputBoxSettings.getCalculatedWidth() - mouseX
                        movingInputBoxWidth = true
                    } else {
                        xDisplacement = mouseX - startX
                        yDisplacement = mouseY - startY
                        movingInputBoxPosition = true
                    }
                }
            }
            it.returnFunction = movingChat || movingInputBox
        }
        EventBus.register<ChatTabClickedEvent> {
            if (!Config.values.movableChatEnabled) {
                return@register
            }
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
            val messagesToDisplay = chatWindow.tabSettings.selectedTab.displayedMessages.size
            if (messagesToDisplay > 0) {
                return@register
            }
            val renderer = chatWindow.renderer
            // render full chat box
            val guiGraphics = it.guiGraphics
            if (moving) {
                guiGraphics.fill0(
                    renderer.internalX.toFloat(),
                    renderer.internalY - renderer.getTotalLineHeight(),
                    renderer.backgroundWidthEndX.toFloat(),
                    renderer.internalY.toFloat(),
                    chatWindow.generalSettings.getUpdatedBackgroundColor()
                )
                renderMoving(
                    guiGraphics.pose(),
                    guiGraphics,
                    renderer.internalX,
                    renderer.internalY,
                    renderer.getTotalLineHeight().roundToInt(),
                    renderer.internalWidth,
                    it.chatWindow == ChatManager.selectedWindow
                )
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
            // area above highest chat line
            val poseStack = guiGraphics.pose()
            val totalLineHeightScaled = renderer.getTotalLineHeight() / renderer.scale
            val visibleLineHeightScaled = (renderer.getLinesPerPageScaled() - it.displayMessageIndex) * renderer.lineHeight
            var startY = when (chatWindow.generalSettings.messageDirection) {
                MessageDirection.TOP_DOWN -> renderer.rescaledY - visibleLineHeightScaled
                MessageDirection.BOTTOM_UP -> renderer.rescaledY - totalLineHeightScaled
            }
            var endY = when (chatWindow.generalSettings.messageDirection) {
                MessageDirection.TOP_DOWN -> renderer.rescaledY
                MessageDirection.BOTTOM_UP -> startY + visibleLineHeightScaled
            }
            poseStack.guiForward(GraphicsUtil.GuiForwardType.MovableChatMoving)
            guiGraphics.fill0(
                renderer.rescaledX,
                startY,
                renderer.rescaledEndX,
                endY,
                chatWindow.generalSettings.getUpdatedBackgroundColor()
            )
            poseStack.createPose {
                val unscaled = 1 / renderer.scale
                poseStack.scale(unscaled, unscaled, 1f)
                renderMoving(
                    poseStack,
                    guiGraphics,
                    renderer.internalX,
                    renderer.internalY,
                    renderer.getTotalLineHeight().roundToInt(),
                    renderer.internalWidth,
                    it.chatWindow == ChatManager.selectedWindow
                )
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
            val allowOutside = Config.values.allowWindowsOutsideScreen
            if (movingChatWidth) {
                val currentInternalX = renderer.internalX
                val newWidthRaw = (mouseX + xDisplacement) - renderer.getUpdatedX()
                val maxWidth = if (allowOutside) {
                    9999.0 // allow large width when outside screen
                } else {
                    Minecraft.getInstance().window.guiScaledWidth - renderer.getUpdatedX().toDouble()
                }
                val newWidth: Double = Mth.clamp(newWidthRaw, MIN_WIDTH.toDouble(), maxWidth)
                val width = newWidth.roundToInt()
                renderer.width = width
                // After changing width, re-anchor x so the left edge stays fixed.
                // The width setter updates internalWidth, so this call uses the new width
                // to compute the correct anchor-relative x that preserves currentInternalX.
                renderer.x = currentInternalX
            }
            if (movingChatHeight) {
                val currentInternalY = renderer.getUpdatedY()
                val newHeightRaw = currentInternalY - (mouseY + yDisplacement)
                val maxHeight = if (allowOutside) {
                    9999.0 // allow large height when outside screen
                } else {
                    renderer.getMaxHeightScaled(HeightType.RAW).toDouble()
                }
                val newHeight: Double = Mth.clamp(
                    newHeightRaw,
                    renderer.getMinHeight().toDouble(),
                    maxHeight
                )
                val height = newHeight.roundToInt()
                val heightNormalized = renderer.getNormalizedHeight(height)
                renderer.height = heightNormalized
                // After changing height, re-anchor y so the bottom edge stays fixed.
                // The height setter updates internalHeight, so this call uses the new height
                // to compute the correct anchor-relative y that preserves currentInternalY.
                renderer.y = currentInternalY
            }
            if (movingChatBox && dragging) {
                val newX = (mouseX - xDisplacement).roundToInt()
                val newYRaw = (mouseY - yDisplacement).roundToInt()
                renderer.x = if (allowOutside) {
                    newX
                } else {
                    Mth.clamp(
                        newX,
                        0,
                        Minecraft.getInstance().window.guiScaledWidth - renderer.getUpdatedWidthValue()
                    )
                }
                val newY = if (allowOutside) {
                    newYRaw
                } else {
                    val minYScaled = renderer.getMinYScaled()
                    val maxYScaled = renderer.getMaxYScaled()
                    Mth.clamp(newYRaw, minYScaled, maxYScaled)
                }
                renderer.y = newY
            }
            if (movingChat) {
                renderer.updateCachedDimension()
            }
            val guiGraphics = it.guiGraphics
            if (movingTab) {
                val selectedTab = ChatManager.globalSelectedTab
                val movingTabIndex: Int = chatWindow.tabSettings.tabs.indexOf(selectedTab)
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
                    for (otherTab in chatWindow.tabSettings.tabs) {
                        if (otherTab === selectedTab) {
                            continue
                        }
                        val tabIndex = chatWindow.tabSettings.tabs.indexOf(otherTab)
                        val movingLeft = tabIndex < movingTabIndex
                        val otherTabMiddleX = otherTab.xStart + otherTab.width / 2.0
                        val leftSwap = movingLeft && selectedTab.xStart < otherTabMiddleX
                        val rightSwap = !movingLeft && selectedTab.xEnd > otherTabMiddleX
                        if (leftSwap || rightSwap) {
                            chatWindow.tabSettings.tabs.add(tabIndex, chatWindow.tabSettings.tabs.removeAt(movingTabIndex))
                            chatWindow.tabSettings.selectedTabIndex = tabIndex
                            queueUpdateConfig = true
                            break
                        }
                    }
                }
                if (debug) {
                    renderDebugMoving(guiGraphics, chatWindow)
                }
            }

            // input box
            val inputBoxSettings = Config.values.inputBoxSettings
            if (movingInputBoxWidth) {
                inputBoxSettings.width = Mth.clamp(
                    (mouseX + xDisplacement) - inputBoxSettings.startX,
                    MIN_WIDTH_INPUT_BOX,
                    Minecraft.getInstance().window.guiScaledWidth - inputBoxSettings.startX.toDouble()
                ).roundToInt()
                it.screen as IMixinScreen
                it.screen.callRebuildWidgets()
            }
            if (movingInputBoxPosition) {
                val maxX = Minecraft.getInstance().window.guiScaledWidth - inputBoxSettings.getCalculatedWidth()
                inputBoxSettings.startX = Mth.clamp(
                    (mouseX - xDisplacement).roundToInt(),
                    2,
                    maxX
                )
                val maxY = Minecraft.getInstance().window.guiScaledHeight - PADDED_INPUT_BOX_HEIGHT
                inputBoxSettings.startY = Mth.clamp(
                    (mouseY - yDisplacement).roundToInt(),
                    INPUT_BOX_PADDING,
                    maxY
                )
                if (inputBoxSettings.startY == maxY) {
                    inputBoxSettings.startY = -PADDED_INPUT_BOX_HEIGHT
                }
                it.screen as IMixinScreen
                it.screen.callRebuildWidgets()
            }
            val inputBoxStartX = inputBoxSettings.startX.toFloat()
            val inputBoxStartY = inputBoxSettings.getCalculatedStartY().toFloat()
            if (Config.values.movableChatEnabled && !Config.values.vanillaInputBox) {
                val poseStack = guiGraphics.pose()
                poseStack.createPose {
                    poseStack.guiForward(GraphicsUtil.GuiForwardType.MovableChatMoving)
                    guiGraphics.fill0(
                        inputBoxStartX + inputBoxSettings.getCalculatedWidth() - RENDER_MOVING_SIZE,
                        inputBoxStartY - INPUT_BOX_PADDING,
                        inputBoxStartX + inputBoxSettings.getCalculatedWidth(),
                        inputBoxStartY + PADDED_INPUT_BOX_HEIGHT,
                        if (movingInputBoxWidth) Config.values.movableChatSelectedColor else Config.values.movableChatColor
                    )
                }
            }
            if (debug) {
                guiGraphics.fill0(
                    inputBoxStartX - 2,
                    inputBoxStartY - INPUT_BOX_PADDING,
                    inputBoxStartX + inputBoxSettings.getCalculatedWidth(),
                    inputBoxStartY + PADDED_INPUT_BOX_HEIGHT,
                    0x7FFF0000
                )
            }
        }
        EventBus.register<OnScreenDisplayEvent> {
            if (!Config.values.movableChatEnabled || !Config.values.movableChatShowEnabledOnScreen || !ChatManager.isChatFocused()) {
                return@register
            }
            it.components.add(Component.literal("Movable Chat Enabled").withColor(MOVABLE_CHAT_COLOR).append(Config.values.movableChatKey.getDisplayName(true)))
        }
    }

    private fun createNewWindow(
        chatWindow: ChatWindow,
        selectedTab: ChatTab,
        mouseX: Double,
        mouseY: Double,
    ) {
        ChatPlus.debugLog("Removed $selectedTab from $chatWindow to create new window")
        removeTabFromWindow(chatWindow, selectedTab)
        val oldRenderer = chatWindow.renderer

        val newWindow = chatWindow.clone()
        selectedTab.chatWindow = newWindow
        newWindow.tabSettings.tabs = mutableListOf(selectedTab)

        // creates new window with same x/y as window separated from
        val newRenderer = newWindow.renderer
        val newX = (mouseX - innerTabXOffset).roundToInt()
        var newY = when (chatWindow.tabSettings.position) {
            TOP -> (mouseY + innerTabYOffset + oldRenderer.getTotalLineHeight()).roundToInt() + CHAT_TAB_Y_OFFSET
            BOTTOM -> (mouseY - innerTabYOffset - CHAT_TAB_Y_OFFSET).roundToInt()
        }
        ChatPlus.debugLog("New window at $newX, $newY")
        newRenderer.width = oldRenderer.width
        newRenderer.height = oldRenderer.height
        newRenderer.x = newRenderer.getUpdatedX(newX)
        newRenderer.y = newRenderer.getUpdatedY(newY)
        newRenderer.updateCachedDimension()

        Config.values.chatWindows.add(newWindow)
        resetGlobalSortedTabs()

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
        movingTabYStart = when (chatWindow.tabSettings.position) {
            TOP -> newY - oldRenderer.getTotalLineHeight().roundToInt() - CHAT_TAB_Y_OFFSET - TAB_HEIGHT
            BOTTOM -> newY + CHAT_TAB_Y_OFFSET
        }
        movingTabMouseXStart = mouseX.roundToInt()
        movingTabMouseYStart = mouseY.roundToInt()
        movingTabXOffset = (mouseX - movingTabMouseXStart).roundToInt()
        movingTabYOffset = (mouseY - movingTabMouseYStart).roundToInt()

        EventBus.post(MovableChatCreateNewWindowEvent(selectedTab, newWindow))
    }

    private fun moveTabToWindow(
        selectedTab: ChatTab,
        windowMovedTo: ChatWindow,
        mouseX: Double,
        mouseY: Double,
    ) {
        removeTabFromWindow(ChatManager.selectedWindow, selectedTab)

        val tabSettings = windowMovedTo.tabSettings
        val newStartX = tabSettings.tabs.last().xEnd + CHAT_TAB_X_SPACE
        val oldWidth = tabSettings.getTabBarWidth()

        selectedTab.chatWindow = windowMovedTo
        selectedTab.rescaleChat()
        tabSettings.tabs.add(selectedTab)
        tabSettings.selectedTabIndex = tabSettings.tabs.size - 1
        tabSettings.resetSortedChatTabs()
        ChatWindowsManager.selectWindow(windowMovedTo)

        // make sure tab is viewed in same place but with offset based on new window
        movingTabMouseXStart = windowMovedTo.renderer.internalX + oldWidth + CHAT_TAB_X_SPACE + innerTabXOffset
        movingTabMouseYStart = when (tabSettings.position) {
            TOP -> windowMovedTo.renderer.internalY - windowMovedTo.renderer.getTotalLineHeight().roundToInt() - CHAT_TAB_Y_OFFSET - innerTabYOffset
            BOTTOM -> windowMovedTo.renderer.internalY + CHAT_TAB_Y_OFFSET + innerTabYOffset
        }
        movingTabXStart = newStartX
        movingTabYStart = tabSettings.tabs.first().yStart
        movingTabXOffset = (mouseX - movingTabMouseXStart).roundToInt()
        movingTabYOffset = (mouseY - movingTabMouseYStart).roundToInt()
        selectedTab.xStart = newStartX
        selectedTab.yStart = tabSettings.tabs.last().yStart
        movingChatBox = false

        EventBus.post(MovableChatTabToWindowEvent(selectedTab, windowMovedTo))
    }

    private fun renderDebugTab(
        guiGraphics: GuiGraphics,
        chatTab: ChatTab,
        outsideTabBar: Boolean,
    ) {
        val poseStack = guiGraphics.pose()
        poseStack.createPose {
            // below cursor - if tab is outside tab bar
            poseStack.guiForward(GraphicsUtil.GuiForwardType.MovableChatDebug)
            guiGraphics.drawString0(
                "$outsideTabBar",
                lastMouseX + 5,
                lastMouseY + 45,
                0xFF5050
            )
            guiGraphics.drawString0(
                "$innerTabXOffset | $innerTabYOffset",
                lastMouseX + 5,
                lastMouseY + 55,
                0xFF5050
            )
        }
        poseStack.createPose {
            // below cursor - offset from tab start position
            poseStack.translate0(x = chatTab.xStart, y = chatTab.yStart, z = 100)
            guiGraphics.drawString0(
                "movingTabXOffset: $movingTabXOffset",
                30,
                -20,
                0xFF5050
            )
            guiGraphics.drawString0(
                "movingTabYOffset: $movingTabYOffset",
                30,
                -10,
                0xFF5050
            )
        }
    }

    private fun renderDebugMoving(
        guiGraphics: GuiGraphics,
        chatWindow: ChatWindow,
    ) {
        val poseStack = guiGraphics.pose()
        val renderer = chatWindow.renderer
        poseStack.createPose {
            poseStack.translate0(z = 1000)
            // tab mouse start position (bottom of cursor)
            guiGraphics.drawString0(
                "$movingTabMouseXStart",
                lastMouseX + 5,
                lastMouseY + 25,
                0xFF5050
            )
            guiGraphics.drawString0(
                "$movingTabMouseYStart",
                lastMouseX + 5,
                lastMouseY + 35,
                0xFF5050
            )
            // exit tab bar
            Config.values.chatWindows.forEach { window ->
                val smallWidth = window === ChatManager.selectedWindow && isSingleTabWindow(window)
                val width = if (smallWidth) window.tabSettings.getTabBarWidth() else window.renderer.backgroundWidthEndX - window.renderer.internalX
                guiGraphics.renderOutline(
                    window.renderer.internalX - MOVE_PADDING_X,
                    getTabStartY(window) - MOVE_PADDING_Y,
                    (width + MOVE_PADDING_X * 2),
                    TAB_HEIGHT + MOVE_PADDING_Y * 2,
                    (0xFFFFFF00).toInt()
                )
            }
            // enter tab bar
            Config.values.chatWindows.forEach { window ->
                val selected = window === ChatManager.selectedWindow && isSingleTabWindow(window)
                val width = if (selected) window.tabSettings.getTabBarWidth() else window.renderer.backgroundWidthEndX - window.renderer.internalX
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
        backgroundWidth: Int,
        selectedWindow: Boolean,
    ) {
        poseStack.createPose {
            val movingWidth = movingChatWidth && selectedWindow
            val movingHeight = movingChatHeight && selectedWindow
            poseStack.guiForward()
            guiGraphics.fill0(
                x + backgroundWidth - RENDER_MOVING_SIZE,
                y - height.toFloat(),
                x + backgroundWidth.toFloat(),
                y.toFloat(),
                if (movingWidth) Config.values.movableChatSelectedColor else Config.values.movableChatColor
            )
            poseStack.guiForward(backwards = movingWidth && !movingHeight)
            guiGraphics.fill0(
                x.toFloat(),
                y - height.toFloat(),
                x + backgroundWidth.toFloat(),
                y - height + RENDER_MOVING_SIZE,
                if (movingHeight) Config.values.movableChatSelectedColor else Config.values.movableChatColor
            )
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
        paddingY: Int = MOVE_PADDING_Y,
    ): RelativeMouseTabBarPosition {
        val renderer = chatWindow.renderer
        val barStartX = renderer.internalX - paddingX
        val barEndX =
            (if (isSingleTabWindow(chatWindow)) {
                renderer.internalX + chatWindow.tabSettings.getTabBarWidth()
            } else {
                renderer.rescaledEndX
            }).toFloat() * renderer.scale + paddingX
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

    private fun isSingleTabWindow(chatWindow: ChatWindow) = chatWindow.tabSettings.tabs.size == 1

    private fun getWindowMovedTo(
        chatWindow: ChatWindow,
        mouseX: Double,
        mouseY: Double,
    ): ChatWindow? {
        Config.values.chatWindows
            .reversed()
            .filter { it !== chatWindow }
            .filter { !it.generalSettings.disabled }
            .forEach { otherWindow ->
                val otherRenderer = otherWindow.renderer
                val insideX = otherRenderer.internalX < mouseX && mouseX < otherRenderer.backgroundWidthEndX
                val insideY = getTabStartY(otherWindow) < mouseY && mouseY < getTabEndY(otherWindow)
                if (insideX && insideY) {
                    return otherWindow
                }
            }
        return null
    }

    private fun removeTabFromWindow(
        chatWindow: ChatWindow,
        selectedTab: ChatTab,
    ) {
        chatWindow.tabSettings.tabs.remove(selectedTab)
        val emptyWindow = chatWindow.tabSettings.tabs.isEmpty()
        if (emptyWindow) {
            Config.values.chatWindows.remove(chatWindow)
        } else {
            chatWindow.tabSettings.selectedTabIndex = max(0, chatWindow.tabSettings.selectedTabIndex - 1)
            chatWindow.tabSettings.startRenderTabIndex = Mth.clamp(chatWindow.tabSettings.startRenderTabIndex, 0, chatWindow.tabSettings.tabs.size - 1)
            chatWindow.tabSettings.resetSortedChatTabs()
        }
        EventBus.post(MovableChatRemoveTabFromWindowEvent(selectedTab, chatWindow, emptyWindow))
    }

    private fun getTabStartY(chatWindow: ChatWindow): Int {
        return when (chatWindow.tabSettings.position) {
            TOP -> chatWindow.renderer.internalY - chatWindow.renderer.getTotalLineHeight().roundToInt() - TAB_HEIGHT - CHAT_TAB_Y_OFFSET
            BOTTOM -> chatWindow.renderer.internalY + CHAT_TAB_Y_OFFSET
        }
    }

    private fun getTabEndY(chatWindow: ChatWindow): Int {
        return when (chatWindow.tabSettings.position) {
            TOP -> chatWindow.renderer.internalY - chatWindow.renderer.getTotalLineHeight().roundToInt() - CHAT_TAB_Y_OFFSET
            BOTTOM -> chatWindow.renderer.internalY + CHAT_TAB_Y_OFFSET + TAB_HEIGHT
        }
    }

    enum class RelativeMouseTabBarPosition {

        INSIDE,
        OUTSIDE_SCREEN,
        OUTSIDE_LEFT,
        OUTSIDE_RIGHT,
        OUTSIDE_TOP,
        OUTSIDE_BOTTOM,

    }

    @Serializable
    data class InputBoxSettings(
        var startX: Int = 2,
        var startY: Int = -PADDED_INPUT_BOX_HEIGHT,
        var width: Int = -1,
        var normalizeInputWhileTyping: Boolean = false,
        var maxInputBoxInputLength: Int = 256 * 5,
        var showInputBoxInputLength: Boolean = true,
        var showInputBoxInputLengthBackgroundColor: Int = Color(0, 0, 0, 0).rgb,
    ) {

        fun getCalculatedWidth(): Int {
            return if (width < 0) {
                Minecraft.getInstance().window.guiScaledWidth - startX
            } else {
                width.coerceAtMost(Minecraft.getInstance().window.guiScaledWidth - startX)
            }
        }

        fun getCalculatedStartY(): Int {
            var start = startY
            if (start < 0) {
                start += Minecraft.getInstance().window.guiScaledHeight
            }
            if (start < INPUT_BOX_PADDING) {
                start = INPUT_BOX_PADDING
            }
            if (start > Minecraft.getInstance().window.guiScaledHeight - PADDED_INPUT_BOX_HEIGHT) {
                start = Minecraft.getInstance().window.guiScaledHeight - PADDED_INPUT_BOX_HEIGHT
            }
            return start
        }

        fun renderBottom(): Boolean {
            val calculatedStartY = getCalculatedStartY()
            return Minecraft.getInstance().window.guiScaledHeight - calculatedStartY < calculatedStartY
        }

        companion object {
            const val INPUT_BOX_PADDING = 4
            const val PADDED_INPUT_BOX_HEIGHT = EDIT_BOX_HEIGHT - INPUT_BOX_PADDING // center ish height
        }

    }

}

data class MovableChatTabToWindowEvent(
    val chatTab: ChatTab,
    val chatWindow: ChatWindow,
)

data class MovableChatCreateNewWindowEvent(
    val chatTab: ChatTab,
    val chatWindow: ChatWindow,
)

data class MovableChatRemoveTabFromWindowEvent(
    val chatTab: ChatTab,
    val chatWindow: ChatWindow,
    val deleted: Boolean,
)