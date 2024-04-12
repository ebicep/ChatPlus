package com.ebicep.chatplus.features

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.hud.*
import net.minecraft.client.gui.screens.Screen
import java.awt.Color

object SelectChat {

    private val SELECT_COLOR = Color(186, 211, 252, 255).rgb
    private var rightClicking = false
    private var lastSelected: ChatTab.ChatPlusGuiMessageLine? = null
    var selectedMessages: MutableSet<ChatTab.ChatPlusGuiMessageLine> = mutableSetOf()

    fun getSelectedMessagesOrdered(): List<ChatTab.ChatPlusGuiMessageLine> {
        return selectedMessages.sortedWith(
            compareBy<ChatTab.ChatPlusGuiMessageLine> {
                ChatManager.selectedTab.messages.indexOf(it.linkedMessage)
            }.thenBy {
                it.wrappedIndex
            }
        )
    }

    init {
        EventBus.register<ChatScreenCloseEvent> {
            selectedMessages.clear()
            lastSelected = null
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            rightClicking = it.button == 1
            if (!rightClicking) {
                selectedMessages.clear()
                return@register
            }
            ChatManager.selectedTab.getHoveredOverMessage()?.let { message ->
                if (Screen.hasShiftDown() && lastSelected != null) {
                    val displayedMessages = ChatManager.selectedTab.displayedMessages
                    val lastSelectedIndex = displayedMessages.indexOf(lastSelected)
                    val messageIndex = displayedMessages.indexOf(message)
                    for (i in minOf(lastSelectedIndex, messageIndex)..maxOf(lastSelectedIndex, messageIndex)) {
                        val displayedMessage = displayedMessages[i]
                        if (!selectedMessages.contains(displayedMessage)) {
                            selectedMessages += displayedMessage
                        }
                    }
                } else {
                    if (selectedMessages.contains(message)) {
                        selectedMessages -= message
                    } else {
                        selectedMessages += message
                        lastSelected = message
                    }
                }
            }
        }
        EventBus.register<ChatScreenMouseReleasedEvent> {
            rightClicking = false
        }
        EventBus.register<ChatScreenMouseDraggedEvent> {
            if (!rightClicking) {
                return@register
            }
            ChatManager.selectedTab.getHoveredOverMessage()?.let { message ->
                if (!selectedMessages.contains(message)) {
                    selectedMessages += message
                }
            }
        }
        EventBus.register<ChatRenderPreLineAppearanceEvent>(3) {
            if (selectedMessages.contains(it.chatPlusGuiMessageLine)) {
                it.backgroundColor = SELECT_COLOR
            }
        }
    }

}