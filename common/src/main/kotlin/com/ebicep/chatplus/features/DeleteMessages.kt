package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatScreenInputEvent
import java.awt.Color

object DeleteMessages {

    val DEFAULT_COLOR = Color(100, 100, 100, 255).rgb

    init {
        var deleteMessageCooldown: Long = -1
        var messageDeleted = false
        EventBus.register<ChatScreenInputEvent>({ 1 }, { messageDeleted }) {
            if (!Config.values.deleteMessageEnabled) {
                return@register
            }
            val canCopyMessage = deleteMessageCooldown < Events.currentTick && Config.values.deleteMessageKey.isDown()
            if (!canCopyMessage) {
                messageDeleted = false
                return@register
            }
            val tab = ChatManager.globalSelectedTab
            val hoveredOverMessage: ChatTab.ChatPlusGuiMessageLine? = tab.getHoveredOverMessageLine()
            val selectedMessages = SelectChat.getAllSelectedMessages()
            if (hoveredOverMessage != null && selectedMessages.isEmpty()) {
                messageDeleted = true
                deleteMessageCooldown = Events.currentTick + 5
                tab.displayedMessages.remove(hoveredOverMessage)
                tab.messages.remove(hoveredOverMessage.linkedMessage)
            } else if (selectedMessages.isNotEmpty()) {
                messageDeleted = true
                deleteMessageCooldown = Events.currentTick + 5
                tab.displayedMessages.removeAll(selectedMessages)
                tab.messages.removeAll(selectedMessages.map { it.linkedMessage }.toSet())
            }
        }
    }

}