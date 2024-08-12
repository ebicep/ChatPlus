package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.ChatPlusTickEvent
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth


const val CHAT_TAB_HEIGHT = 15
const val CHAT_TAB_Y_OFFSET = 1 // offset from text box
const val CHAT_TAB_X_SPACE = 1 // space between categories

data class ChatTabClickedEvent(val chatTab: ChatTab, val mouseX: Double, val tabXStart: Double)

data class ChatTabRenderEvent(val poseStack: PoseStack, val chatTab: ChatTab, val tabWidth: Int, var xStart: Double)

data class ChatTabSwitchEvent(val oldTab: ChatTab, val newTab: ChatTab)

object ChatTabs {

    val defaultTab: ChatTab = ChatTab("All", "(?s).*", alwaysAdd = true)
    private var startRenderTabIndex = 0

    init {
        EventBus.register<ChatPlusTickEvent> {
            if (!Config.values.chatTabsEnabled) {
                checkTabRefresh(defaultTab)
            } else {
                Config.values.chatTabs.forEach { checkTabRefresh(it) }
            }
        }
        EventBus.register<ChatRenderPreLinesEvent>({ 100 }) {
            if (!Config.values.chatTabsEnabled) {
                return@register
            }
            val chatFocused: Boolean = ChatManager.isChatFocused()
            if (chatFocused) {
                renderTabs(it.guiGraphics, ChatRenderer.x.toDouble(), ChatRenderer.y.toDouble())
            }
        }
        EventBus.register<ChatScreenKeyPressedEvent> {
            if (!Config.values.chatTabsEnabled || !Config.values.arrowCycleTabEnabled) {
                return@register
            }
            it.screen as IMixinChatScreen
            if (it.screen.input.value.isNotEmpty()) {
                return@register
            }
            val keyCode = it.keyCode
            if (keyCode == 263) { // left arrow
                scrollTab(-1)
            } else if (keyCode == 262) { // right arrow
                scrollTab(1)
            }
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            if (!Config.values.chatTabsEnabled) {
                return@register
            }
            if (it.button == 0) {
                handleClickedTab(it.mouseX, it.mouseY)
            }
        }
        EventBus.register<GetMaxHeightEvent> {
            if (!Config.values.chatTabsEnabled) {
                return@register
            }
            it.maxHeight -= CHAT_TAB_HEIGHT
        }
        EventBus.register<GetDefaultYEvent> {
            if (!Config.values.chatTabsEnabled) {
                return@register
            }
            it.y -= CHAT_TAB_HEIGHT
        }
        // tab auto prefix
        EventBus.register<ChatScreenSendMessagePreEvent> {
            it.message = ChatManager.selectedTab.autoPrefix + it.message
        }
        // moving tabs
        ChatTabsMover
    }

    // negative = scroll left , positive = scroll right
    private fun scrollTab(amount: Int) {
        if (amount < 0 && startRenderTabIndex > 0) {
            startRenderTabIndex--
        } else if (amount > 0 && startRenderTabIndex < Config.values.chatTabs.size - 1) {
            // check if last tab is visible
            var totalWidth = 0
            Config.values.chatTabs.forEachIndexed { index, it ->
                if (index < startRenderTabIndex) {
                    return@forEachIndexed
                }
                totalWidth += it.width + CHAT_TAB_X_SPACE
            }
            if (totalWidth >= Minecraft.getInstance().window.guiScaledWidth) {
                startRenderTabIndex++
            }
        }
        if (Config.values.moveToTabWhenCycling) {
            Config.values.selectedTab = Mth.clamp(Config.values.selectedTab + amount, 0, Config.values.chatTabs.size - 1)
        }
    }

    private fun checkTabRefresh(it: ChatTab) {
        if (it.resetDisplayMessageAtTick == Events.currentTick) {
            it.refreshDisplayMessages()
        }
    }

    private fun handleClickedTab(x: Double, y: Double) {
        val translatedY = ChatManager.getY() - y
        ChatPlus.LOGGER.info("translatedY: $translatedY")
        if (translatedY > CHAT_TAB_Y_OFFSET - 4 || translatedY < -(9 + ChatTab.PADDING + ChatTab.PADDING)) {
            return
        }
        Config.values.chatTabs.forEachIndexed { index, it ->
            if (index < startRenderTabIndex) {
                return@forEachIndexed
            }
            val insideTabX = it.xStart < x && x < it.xEnd
            if (insideTabX) {
                EventBus.post(ChatTabClickedEvent(it, x, it.xStart))
                if (it != ChatManager.selectedTab) {
                    val oldTab = ChatManager.selectedTab
                    oldTab.resetFilter()
                    Config.values.selectedTab = index
                    queueUpdateConfig = true
                    ChatManager.selectedTab.queueRefreshDisplayedMessages(false)
                    EventBus.post(ChatTabSwitchEvent(oldTab, it))
                    return
                }
            }
        }
    }

    private fun renderTabs(guiGraphics: GuiGraphics, x: Double, y: Double) {
        val poseStack = guiGraphics.pose()
        var xStart = x
        val yStart = y + CHAT_TAB_Y_OFFSET
        poseStack.createPose {
            poseStack.translate0(y = yStart)
            Config.values.chatTabs.forEachIndexed { index, it ->
                if (index < startRenderTabIndex) {
                    return@forEachIndexed
                }
                poseStack.createPose {
                    val tabWidth = it.width

                    val translateX = EventBus.post(ChatTabRenderEvent(poseStack, it, tabWidth, xStart)).xStart
                    poseStack.translate0(x = translateX)

                    it.xStart = translateX
                    it.y = yStart

                    renderTab(it, guiGraphics)

                    xStart += tabWidth + CHAT_TAB_X_SPACE
                }
            }
        }
    }

    private fun renderTab(chatTab: ChatTab, guiGraphics: GuiGraphics) {
        val poseStack = guiGraphics.pose()
        val isSelected = chatTab == ChatManager.selectedTab
        val backgroundOpacity = ((if (isSelected) 255 else 100) * ChatManager.getBackgroundOpacity()).toInt() shl 24
        val textColor = if (isSelected) 0xffffff else 0x999999

        poseStack.createPose {
            poseStack.guiForward()
            guiGraphics.fill(
                0,
                0,
                chatTab.width,
                9 + ChatTab.PADDING + ChatTab.PADDING,
                backgroundOpacity
            )
            poseStack.guiForward()
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                chatTab.name,
                ChatTab.PADDING,
                ChatTab.PADDING + ChatTab.PADDING / 2,
                textColor
            )
        }
    }


}


