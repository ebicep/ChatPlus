package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.ChatPlusTickEvent
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.translateX
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics


const val CHAT_TAB_HEIGHT = 29
const val CHAT_TAB_Y_OFFSET = 1 // offset from text box
const val CHAT_TAB_X_SPACE = 1 // space between categories

object ChatTabs {

    val defaultTab = ChatTab("All", "(?s).*")

    init {
        EventBus.register<ChatPlusTickEvent> {
            if (!Config.values.chatTabsEnabled) {
                checkTabRefresh(defaultTab)
            } else {
                Config.values.chatTabs.forEach { checkTabRefresh(it) }
            }
        }
        EventBus.register<ChatRenderPreLinesEvent>(100)
        {
            if (!Config.values.chatTabsEnabled) {
                return@register
            }
            val chatFocused: Boolean = ChatManager.isChatFocused()
            if (chatFocused) {
                renderTabs(it.guiGraphics, ChatRenderer.x, ChatRenderer.y)
            }
        }
        EventBus.register<ChatScreenMouseClickedEvent>
        {
            if (!Config.values.chatTabsEnabled) {
                return@register
            }
            if (it.button == 0) {
                handleClickedTab(it.mouseX, it.mouseY)
            }
        }
        EventBus.register<GetMaxHeightEvent>
        {
            if (!Config.values.chatTabsEnabled) {
                return@register
            }
            it.maxHeight -= CHAT_TAB_HEIGHT
        }
        EventBus.register<GetDefaultYEvent>
        {
            if (!Config.values.chatTabsEnabled) {
                return@register
            }
            it.y = -CHAT_TAB_HEIGHT
        }
    }

    private fun checkTabRefresh(it: ChatTab) {
        if (it.resetDisplayMessageAtTick == Events.currentTick) {
            it.refreshDisplayedMessage()
        }
    }

    private fun handleClickedTab(x: Double, y: Double) {
        val translatedY = ChatManager.getY() - y
        var xOff = 0.0
        val font = Minecraft.getInstance().font
        //ChatPlus.LOGGER.debug("x: $x, translatedY: $translatedY")
        if (translatedY > CHAT_TAB_Y_OFFSET || translatedY < -(9 + ChatTab.PADDING + ChatTab.PADDING)) {
            return
        }
        Config.values.chatTabs.forEachIndexed { index, it ->
            val categoryLength = font.width(it.name) + ChatTab.PADDING + ChatTab.PADDING
            if (x > xOff && x < xOff + categoryLength && it != ChatManager.selectedTab) {
                Config.values.selectedTab = index
                Config.save()
                ChatManager.selectedTab.refreshDisplayedMessage()
                return
            }
            xOff += categoryLength + CHAT_TAB_X_SPACE
        }
    }


    private fun renderTabs(guiGraphics: GuiGraphics, x: Int, y: Int) {
        val poseStack = guiGraphics.pose()
        poseStack.createPose {
            poseStack.translate(x.toFloat(), y.toFloat() + CHAT_TAB_Y_OFFSET, 0f)
            Config.values.chatTabs.forEach {
                it.render(guiGraphics)
                poseStack.translateX(
                    ChatTab.PADDING +
                            Minecraft.getInstance().font.width(it.name).toFloat() +
                            ChatTab.PADDING +
                            CHAT_TAB_X_SPACE
                )
            }
        }
    }
}

