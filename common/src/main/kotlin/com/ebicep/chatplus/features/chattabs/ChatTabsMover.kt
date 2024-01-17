package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatScreenMouseDraggedEvent
import com.ebicep.chatplus.hud.ChatScreenMouseReleasedEvent
import com.ebicep.chatplus.util.GraphicsUtil.guiForward

class TabInfo(var xStart: Double, var width: Int) {

    var xMiddle: Double = (xStart + width) / 2
    var xEnd: Double = xStart + width

    fun update(newStart: Double) {
        xStart = newStart
        xMiddle = xStart + width.toDouble() / 2
        xEnd = xStart + width
    }
}

object ChatTabsMover {

    private var movingTab: Boolean = false
    private var movingTabMouseStart: Double = 0.0
    private var movingTabXOffset: Double = 0.0
    private var movingTabXStart: Double = 0.0
    private var chatTabPositions: MutableMap<ChatTab, TabInfo> = mutableMapOf()

    init {
        EventBus.register<ChatScreenMouseDraggedEvent> {
            if (!movingTab) {
                return@register
            }
            val selectedTab = ChatManager.selectedTab
            val movingTabPosition = chatTabPositions[selectedTab] ?: return@register
            val movingTabIndex: Int = Config.values.chatTabs.indexOf(selectedTab)
            if (movingTabIndex == -1) {
                return@register
            }
            movingTabXOffset = it.mouseX - movingTabMouseStart
            for (otherTab in chatTabPositions) {
                val chatTab = otherTab.key
                val chatTabPosition = otherTab.value
                if (chatTab === selectedTab) {
                    continue
                }
                val tabIndex = Config.values.chatTabs.indexOf(chatTab)
                val movingLeft = tabIndex < movingTabIndex
                val leftSwap = movingLeft && movingTabPosition.xStart < chatTabPosition.xMiddle
                val rightSwap = !movingLeft && movingTabPosition.xEnd > chatTabPosition.xMiddle
                if (leftSwap || rightSwap) {
                    Config.values.chatTabs.add(tabIndex, Config.values.chatTabs.removeAt(movingTabIndex))
                    Config.values.selectedTab = tabIndex
                    queueUpdateConfig = true
                    chatTabPositions.clear()
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
            chatTabPositions.computeIfAbsent(it.chatTab) { _ -> TabInfo(it.xStart, it.tabWidth) }.update(it.xStart)
        }
    }


}