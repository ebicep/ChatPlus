package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.*
import com.ebicep.chatplus.features.textbarelements.ShowBookmarksBarElement
import com.ebicep.chatplus.features.textbarelements.ShowBookmarksToggleEvent
import com.ebicep.chatplus.features.textbarelements.TextBarElements
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.mixin.IMixinScreen
import net.minecraft.client.gui.screens.ChatScreen
import java.util.*

object BookmarkMessages {

    private val bookmarkedMessages: MutableSet<ChatTab.ChatPlusGuiMessage> = Collections.newSetFromMap(IdentityHashMap())
    var showingBoomarks = false

    init {
        EventBus.register<TextBarElements.AddTextBarElementEvent>({ 50 }) {
            if (!Config.values.bookmarkEnabled) {
                return@register
            }
            if (Config.values.bookmarkTextBarElementEnabled) {
                it.elements.add(ShowBookmarksBarElement(it.screen))
            }
        }
        EventBus.register<ChatTabRemoveMessageEvent> {
            bookmarkedMessages.remove(it.guiMessage)
        }
        EventBus.register<ChatTabAddNewMessageEvent> {
            val content = it.component.string
            for (autoBookMarkPattern in Config.values.autoBookMarkPatterns) {
                if (autoBookMarkPattern.matches(content)) {
                    bookmarkedMessages.add(it.guiMessage)
                    return@register
                }
            }
        }
        var showBookmarkShortcutUsed = false
        EventBus.register<ChatScreenKeyPressedEvent>({ 1 }, { showBookmarkShortcutUsed }) {
            var toggledBookmarkMessage = false
            if (Config.values.bookmarkKey.isDown()) {
                val hoveredOverMessage = ChatManager.globalSelectedTab.getHoveredOverMessageLine()
                if (hoveredOverMessage != null && SelectChat.selectedMessages.isEmpty()) {
                    toggleMessageBookmark(hoveredOverMessage.linkedMessage)
                    toggledBookmarkMessage = true
                } else if (SelectChat.selectedMessages.isNotEmpty()) {
                    SelectChat.selectedMessages.forEach {
                        toggleMessageBookmark(it.linkedMessage)
                    }
                    toggledBookmarkMessage = true
                }
            }
            if (!toggledBookmarkMessage && Config.values.bookmarkTextBarElementKey.isDown()) {
                showBookmarkShortcutUsed = true
                toggle(it.screen)
                it.returnFunction = true
            }
        }
        EventBus.register<ChatScreenCloseEvent> {
            if (showingBoomarks) {
                showingBoomarks = false
                ChatManager.globalSelectedTab.resetFilter()
            }
        }
        EventBus.register<ChatTabRewrapDisplayMessages> {
            showingBoomarks = false
            ChatManager.globalSelectedTab.resetFilter()
        }
        EventBus.register<ChatTabRefreshDisplayMessages> {
            if (showingBoomarks && bookmarkedMessages.isNotEmpty()) {
                it.predicates.add { guiMessage -> bookmarkedMessages.contains(guiMessage) }
            }
        }
        EventBus.register<ChatTabSwitchEvent> {
            if (showingBoomarks) {
                ChatManager.globalSelectedTab.queueRefreshDisplayedMessages(false)
            }
        }
        EventBus.register<ChatTabAddDisplayMessageEvent> {
            if (showingBoomarks && ChatManager.isChatFocused()) {
                it.filtered = true
                it.addMessage = bookmarkedMessages.contains(it.linkedMessage)
            }
        }
        EventBus.register<ChatRenderPreLineAppearanceEvent>({ Config.values.bookmarkLinePriority }) {
            if (bookmarkedMessages.contains(it.chatPlusGuiMessageLine.linkedMessage)) {
                it.backgroundColor = Config.values.bookmarkColor
            }
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            if (it.button != 0) {
                return@register
            }
            if (showingBoomarks) {
                ChatManager.globalSelectedTab.getMessageLineAt(it.mouseX, it.mouseY)?.let { message ->
                    showingBoomarks = false
                    ChatManager.globalSelectedTab.moveToMessage(it.screen, message)
                }
            }
        }
    }

    private fun toggleMessageBookmark(guiMessage: ChatTab.ChatPlusGuiMessage) {
        if (bookmarkedMessages.contains(guiMessage)) {
            bookmarkedMessages.remove(guiMessage)
        } else {
            bookmarkedMessages.add(guiMessage)
        }
    }

    fun toggle(chatScreen: ChatScreen) {
        if (!showingBoomarks && bookmarkedMessages.isEmpty()) {
            return
        }
        showingBoomarks = !showingBoomarks
        EventBus.post(ShowBookmarksToggleEvent(!showingBoomarks))
        if (!showingBoomarks) {
            ChatManager.globalSelectedTab.resetFilter()
        } else {
            ChatManager.globalSelectedTab.queueRefreshDisplayedMessages(false)
        }
        chatScreen as IMixinChatScreen
        chatScreen.initial = chatScreen.input!!.value
        chatScreen as IMixinScreen
        chatScreen.callRebuildWidgets()
    }

}