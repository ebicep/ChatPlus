package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.*
import com.ebicep.chatplus.features.chatwindows.ChatTabSwitchEvent
import com.ebicep.chatplus.features.chatwindows.WindowSwitchEvent
import com.ebicep.chatplus.features.textbarelements.AddTextBarElementEvent
import com.ebicep.chatplus.features.textbarelements.ShowBookmarksBarElement
import com.ebicep.chatplus.features.textbarelements.ShowBookmarksToggleEvent
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.mixin.IMixinScreen
import net.minecraft.client.gui.screens.ChatScreen
import java.util.*

object BookmarkMessages {

    private val bookmarkedMessages: MutableSet<ChatTab.ChatPlusGuiMessage> = Collections.newSetFromMap(IdentityHashMap())
    var showingBookmarks = false

    init {
        EventBus.register<AddTextBarElementEvent>({ 50 }) {
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
            if (!Config.values.bookmarkEnabled) {
                return@register
            }
            val content = it.rawComponent
            for (autoBookMarkPattern in Config.values.autoBookMarkPatterns) {
                if (autoBookMarkPattern.matches(content)) {
                    bookmarkedMessages.add(it.chatPlusGuiMessage)
                    return@register
                }
            }
        }
        var toggledBookmarkMessage = false
        EventBus.register<ChatScreenInputEvent>({ 2 }, { toggledBookmarkMessage }) {
            toggledBookmarkMessage = false
            if (!Config.values.bookmarkEnabled) {
                return@register
            }
            if (it.checkRelease(Config.values.bookmarkKey)) {
                return@register
            }
            val hoveredOverMessage = ChatManager.globalSelectedTab.getHoveredOverMessageLine()
            val selectedMessages = SelectChat.getAllSelectedMessages()
            if (hoveredOverMessage != null && selectedMessages.isEmpty()) {
                toggleMessageBookmark(hoveredOverMessage.linkedMessage)
                toggledBookmarkMessage = true
            } else if (SelectChat.selectedMessages.isNotEmpty()) {
                selectedMessages.forEach {
                    toggleMessageBookmark(it.linkedMessage)
                }
                toggledBookmarkMessage = true
            }
        }
        var showBookmarkShortcutUsed = false
        EventBus.register<ChatScreenInputEvent>({ 1 }, { showBookmarkShortcutUsed }) {
            showBookmarkShortcutUsed = false
            if (!Config.values.bookmarkEnabled) {
                return@register
            }
            if (it.checkRelease(Config.values.bookmarkTextBarElementKey)) {
                return@register
            }
            showBookmarkShortcutUsed = true
            toggle(it.screen)
            it.returnFunction = true
        }

        EventBus.register<ChatScreenCloseEvent> {
            if (showingBookmarks) {
                showingBookmarks = false
                ChatManager.globalSelectedTab.resetFilter()
            }
        }
        EventBus.register<ChatTabRewrapDisplayMessages> {
            showingBookmarks = false
            ChatManager.globalSelectedTab.resetFilter()
        }
        EventBus.register<ChatTabRefreshDisplayMessages> {
            if (showingBookmarks && bookmarkedMessages.isNotEmpty()) {
                it.predicates.add { guiMessage -> bookmarkedMessages.contains(guiMessage) }
            }
        }
        EventBus.register<ChatTabSwitchEvent> {
            if (showingBookmarks) {
                ChatManager.globalSelectedTab.queueRefreshDisplayedMessages(false)
            }
        }
        EventBus.register<WindowSwitchEvent> {
            if (showingBookmarks) {
                it.oldWindow.tabSettings.selectedTab.queueRefreshDisplayedMessages(false)
            }
        }
        EventBus.register<ChatTabAddDisplayMessageEvent> {
            if (showingBookmarks && ChatManager.isChatFocused()) {
                it.filtered = true
                it.addMessage = bookmarkedMessages.contains(it.linkedMessage)
            }
        }
        EventBus.register<ChatRenderPreLineAppearanceEvent>({ Config.values.bookmarkLinePriority }) {
            if (!Config.values.bookmarkEnabled) {
                return@register
            }
            if (bookmarkedMessages.contains(it.chatPlusGuiMessageLine.linkedMessage)) {
                it.backgroundColor = Config.values.bookmarkColor
            }
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            if (it.button != 0) {
                return@register
            }
            if (showingBookmarks) {
                ChatManager.globalSelectedTab.getHoveredOverMessageLine(it.mouseX, it.mouseY)?.let { message ->
                    showingBookmarks = false
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
        if (!showingBookmarks && bookmarkedMessages.isEmpty()) {
            return
        }
        showingBookmarks = !showingBookmarks
        EventBus.post(ShowBookmarksToggleEvent(!showingBookmarks))
        if (!showingBookmarks) {
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