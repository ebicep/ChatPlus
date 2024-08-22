package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.hud.*
import net.minecraft.client.gui.screens.Screen
import java.awt.Color
import java.util.*

object SelectChat {

    private val SELECT_COLOR = Color(186, 211, 252, 255).rgb
    private var rightClicking = false
    private var lastSelected: ChatTab.ChatPlusGuiMessageLine? = null
    var selectedMessages: MutableMap<ChatTab, MutableSet<ChatTab.ChatPlusGuiMessageLine>> = mutableMapOf()//Collections.newSetFromMap(IdentityHashMap())

    fun getSelectedMessagesOrdered(): List<ChatTab.ChatPlusGuiMessageLine> {
        val chatTabs: List<ChatTab> = Config.values.chatWindows.flatMap { it.tabs }.toList()
        // return sorted based chatTabs index
        return chatTabs.flatMap { tab ->
            selectedMessages[tab]?.sortedWith(
                compareBy<ChatTab.ChatPlusGuiMessageLine> {
                    tab.messages.indexOf(it.linkedMessage)
                }.thenBy {
                    it.wrappedIndex
                }
            ) ?: emptyList()
        }
    }

    fun getTabSelectedMessages(chatTab: ChatTab): MutableSet<ChatTab.ChatPlusGuiMessageLine> {
        if (selectedMessages[chatTab] == null) {
            selectedMessages[chatTab] = Collections.newSetFromMap(IdentityHashMap())
        }
        return selectedMessages[chatTab]!!
    }

    fun getAllSelectedMessages(): MutableSet<ChatTab.ChatPlusGuiMessageLine> {
        return selectedMessages.values.flatten().toMutableSet()
    }

    init {
        EventBus.register<ChatScreenCloseEvent> {
            selectedMessages.values.forEach { it.clear() }
            lastSelected = null
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            rightClicking = it.button == 1
            if (!rightClicking) {
                selectedMessages.values.forEach { it.clear() }
                return@register
            }
            val selectedTab = ChatManager.globalSelectedTab
            selectedTab.getHoveredOverMessageLine()?.let { message ->
                val selected = getTabSelectedMessages(selectedTab)
                if (Screen.hasShiftDown() && lastSelected != null) {
                    val displayedMessages = selectedTab.displayedMessages
                    val lastSelectedIndex = displayedMessages.indexOf(lastSelected)
                    val messageIndex = displayedMessages.indexOf(message)
                    if (lastSelectedIndex == -1 || messageIndex == -1) {
                        return@register
                    }
                    for (i in minOf(lastSelectedIndex, messageIndex)..maxOf(lastSelectedIndex, messageIndex)) {
                        val displayedMessage = displayedMessages[i]
                        if (!selected.contains(displayedMessage)) {
                            selected += displayedMessage
                        }
                    }
                } else {
                    if (selected.contains(message)) {
                        getTabSelectedMessages(selectedTab) -= message
                    } else {
                        selected += message
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
            val selectedTab = ChatManager.globalSelectedTab
            val selected = getTabSelectedMessages(selectedTab)
            selectedTab.getHoveredOverMessageLine()?.let { message ->
                if (!selected.contains(message)) {
                    selected += message
                }
            }
        }
        EventBus.register<ChatRenderPreLineAppearanceEvent>({ Config.values.selectChatLinePriority }) {
            val selectedTab = it.chatWindow.selectedTab
            val selected = getTabSelectedMessages(selectedTab)
            if (selected.contains(it.chatPlusGuiMessageLine)) {
                it.backgroundColor = SELECT_COLOR
            }
        }
    }

}