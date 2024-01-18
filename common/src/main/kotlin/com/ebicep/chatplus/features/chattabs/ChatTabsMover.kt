package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatScreenMouseDraggedEvent
import com.ebicep.chatplus.hud.ChatScreenMouseReleasedEvent
import com.ebicep.chatplus.util.GraphicsUtil.guiForward

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
            val selectedTab = ChatManager.selectedTab
            val movingTabIndex: Int = Config.values.chatTabs.indexOf(selectedTab)
            if (movingTabIndex == -1) {
                return@register
            }
            movingTabXOffset = it.mouseX - movingTabMouseStart
            for (otherTab in Config.values.chatTabs) {
                if (otherTab === selectedTab) {
                    continue
                }
                val tabIndex = Config.values.chatTabs.indexOf(otherTab)
                val movingLeft = tabIndex < movingTabIndex
                val otherTabMiddleX = otherTab.x + otherTab.width / 2
                val leftSwap = movingLeft && selectedTab.x < otherTabMiddleX
                val rightSwap = !movingLeft && selectedTab.x + selectedTab.width > otherTabMiddleX
                if (leftSwap || rightSwap) {
                    Config.values.chatTabs.add(tabIndex, Config.values.chatTabs.removeAt(movingTabIndex))
                    Config.values.selectedTab = tabIndex
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
            val moving = movingTab && it.chatTab === ChatManager.selectedTab
            if (moving) {
                it.xStart = movingTabXStart + movingTabXOffset
                poseStack.guiForward()
                poseStack.guiForward()
            }
        }
    }


}