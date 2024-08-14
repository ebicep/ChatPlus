package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatScreenMouseDraggedEvent
import com.ebicep.chatplus.hud.ChatScreenMouseReleasedEvent
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import kotlin.math.abs

object ChatTabsMover {

    private var movingTab: Boolean = false
    private var movingTabMouseStart: Double = 0.0
    private var movingTabXOffset: Double = 0.0
    private var movingTabXStart: Double = 0.0

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
            movingTabXOffset = it.mouseX - movingTabMouseStart
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
        EventBus.register<ChatScreenMouseReleasedEvent> {
            if (movingTab) {
                movingTab = false
            }
        }
        EventBus.register<ChatTabClickedEvent> {
            movingTab = true
            movingTabMouseStart = it.mouseX
            movingTabXOffset = 0.0
            movingTabXStart = it.tabXStart
        }
        EventBus.register<ChatTabRenderEvent> {
            val poseStack = it.poseStack
            val moving = movingTab && it.chatTab === ChatManager.globalSelectedTab
            if (moving && abs(movingTabXOffset) > 4) {
                it.xStart = movingTabXStart + movingTabXOffset
                poseStack.guiForward()
                poseStack.guiForward()
            }
        }
    }


}