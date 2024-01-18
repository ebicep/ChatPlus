package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.ChatPlusTickEvent
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics


const val CHAT_TAB_HEIGHT = 15
const val CHAT_TAB_Y_OFFSET = 1 // offset from text box
const val CHAT_TAB_X_SPACE = 1 // space between categories

data class ChatTabClickedEvent(val chatTab: ChatTab, val mouseX: Double, val tabXStart: Double)

data class ChatTabRenderEvent(val poseStack: PoseStack, val chatTab: ChatTab, val tabWidth: Int, var xStart: Double)

object ChatTabs {

    val defaultTab: ChatTab = ChatTab("All", "(?s).*")
    val maxX: Int
        get() = if (true) ChatRenderer.width else Minecraft.getInstance().window.guiScaledWidth

    init {
        EventBus.register<ChatPlusTickEvent> {
            if (!Config.values.chatTabsEnabled) {
                checkTabRefresh(defaultTab)
            } else {
                Config.values.chatTabs.forEach { checkTabRefresh(it) }
            }
        }
        EventBus.register<ChatRenderPreLinesEvent>(100) {
            if (!Config.values.chatTabsEnabled) {
                return@register
            }
            val chatFocused: Boolean = ChatManager.isChatFocused()
            if (chatFocused) {
                renderTabs(it.guiGraphics, ChatRenderer.x.toDouble(), ChatRenderer.y.toDouble())
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

    private fun checkTabRefresh(it: ChatTab) {
        if (it.resetDisplayMessageAtTick == Events.currentTick) {
            it.refreshDisplayedMessage()
        }
    }

    private fun handleClickedTab(x: Double, y: Double) {
        val translatedY = ChatManager.getY() - y
        if (translatedY > CHAT_TAB_Y_OFFSET || translatedY < -(9 + ChatTab.PADDING + ChatTab.PADDING)) {
            return
        }
        Config.values.chatTabs.forEachIndexed { index, it ->
            val insideTabX = it.xStart < x && x < it.xEnd
            if (insideTabX) {
                EventBus.post(ChatTabClickedEvent(it, x, it.xStart))
                if (it != ChatManager.selectedTab) {
                    Config.values.selectedTab = index
                    queueUpdateConfig = true
                    ChatManager.selectedTab.refreshDisplayedMessage()
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
            Config.values.chatTabs.forEach {
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


