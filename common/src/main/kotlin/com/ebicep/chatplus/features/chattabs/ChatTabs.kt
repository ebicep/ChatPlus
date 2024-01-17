package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.ChatPlusTickEvent
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.GraphicsUtil.translateX
import com.ebicep.chatplus.util.GraphicsUtil.translateY
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
                renderTabs(it.guiGraphics, ChatRenderer.x, ChatRenderer.y)
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

    private fun getTabRows(): Int {
        var rows = 0
        var currentX = ChatRenderer.x
        Config.values.chatTabs.forEach {
            val tabWidth = it.getTabWidth()
            val newX = tabWidth + CHAT_TAB_X_SPACE
            if (currentX + newX > maxX) {
                currentX = ChatRenderer.x
                rows++
            } else {
                currentX += newX
            }
        }
        return rows
    }

    private fun checkTabRefresh(it: ChatTab) {
        if (it.resetDisplayMessageAtTick == Events.currentTick) {
            it.refreshDisplayedMessage()
        }
    }

    private fun handleClickedTab(x: Double, y: Double) {
        val translatedY = ChatManager.getY() - y
        var tabXStart = 0.0
        val font = Minecraft.getInstance().font
        if (translatedY > CHAT_TAB_Y_OFFSET || translatedY < -(9 + ChatTab.PADDING + ChatTab.PADDING)) {
            return
        }
        Config.values.chatTabs.forEachIndexed { index, it ->
            val categoryLength = font.width(it.name) + ChatTab.PADDING + ChatTab.PADDING
            val insideTabX = tabXStart < x && x < tabXStart + categoryLength
            if (insideTabX) {
                EventBus.post(ChatTabClickedEvent(it, x, tabXStart))
                if (it != ChatManager.selectedTab) {
                    Config.values.selectedTab = index
                    queueUpdateConfig = true
                    ChatManager.selectedTab.refreshDisplayedMessage()
                    return
                }
            }
            tabXStart += categoryLength + CHAT_TAB_X_SPACE
        }
    }

    private fun renderTabs(guiGraphics: GuiGraphics, x: Int, y: Int) {
        val poseStack = guiGraphics.pose()
        poseStack.createPose {
            poseStack.translate(x.toFloat(), y.toFloat() + CHAT_TAB_Y_OFFSET, 0f)
            var currentX = x.toDouble()
            var currentY = 0.0
            Config.values.chatTabs.forEach {
                poseStack.createPose {
                    val tabWidth = it.getTabWidth()
                    val newX = tabWidth + CHAT_TAB_X_SPACE
                    val nextRow = currentX + newX > maxX
                    if (nextRow) {
                        currentX = x.toDouble()
                        currentY += CHAT_TAB_HEIGHT
                    }

                    poseStack.translateX(EventBus.post(ChatTabRenderEvent(poseStack, it, tabWidth, currentX)).xStart)
                    poseStack.translateY(currentY)

                    renderTab(it, guiGraphics)

                    currentX += newX
                }

            }
        }
    }

    private fun renderTab(chatTab: ChatTab, guiGraphics: GuiGraphics) {
        val mc = Minecraft.getInstance()
        val poseStack = guiGraphics.pose()
        val isSelected = chatTab == ChatManager.selectedTab
        val backgroundOpacity = ((if (isSelected) 255 else 100) * ChatManager.getBackgroundOpacity()).toInt() shl 24
        val textColor = if (isSelected) 0xffffff else 0x999999

        poseStack.createPose {
            poseStack.guiForward()
            guiGraphics.fill(
                0,
                0,
                chatTab.getTabWidth(),
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


