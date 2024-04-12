package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.ChatTabAddDisplayMessageEvent
import com.ebicep.chatplus.features.textbarelements.FindTextBarElement
import com.ebicep.chatplus.features.textbarelements.FindToggleEvent
import com.ebicep.chatplus.features.textbarelements.TextBarElements
import com.ebicep.chatplus.features.textbarelements.TranslateToggleEvent
import com.ebicep.chatplus.hud.*
import net.minecraft.client.Minecraft
import java.awt.Color

object FindText {

    const val FIND_COLOR = (0xFFFFFF55).toInt()
    private val findBackgroundColor = Color(FIND_COLOR).darker().rgb
    var findEnabled: Boolean = false

    init {
        var lastMovedToMessage: Pair<Pair<ChatTab.ChatPlusGuiMessage, Int>, Long>? = null // <linked message, wrapped index>, tick
        EventBus.register<TextBarElements.AddTextBarElementEvent>(5) {
            it.elements.add(FindTextBarElement(it.screen))
        }
        var findShortcutUsed = false
        EventBus.register<ChatScreenKeyPressedEvent>(1, { findShortcutUsed }) {
            findShortcutUsed = Config.values.keyFindMessageWithModifier.isDown()
            if (findShortcutUsed) {
                toggle(it.screen)
                it.returnFunction = true
            }
        }
        EventBus.register<ChatScreenCloseEvent> {
            findEnabled = false
        }
        EventBus.register<ChatScreenInputBoxEditEvent> {
            if (findEnabled) {
                ChatManager.selectedTab.refreshDisplayedMessage(it.str)
                it.returnFunction = true
            }
        }
        EventBus.register<TranslateToggleEvent> {
            if (findEnabled) {
                findEnabled = false
            }
        }
        EventBus.register<ChatTabAddDisplayMessageEvent> {
            val screen = Minecraft.getInstance().screen
            if (findEnabled && screen is ChatPlusScreen) {
                val filter = screen.input?.value
                if (filter != null && !it.component.string.lowercase().contains(filter.lowercase())) {
                    it.returnFunction = true
                }
            }
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            if (it.button != 0) {
                return@register
            }
            if (findEnabled) {
                ChatManager.selectedTab.getMessageAt(it.mouseX, it.mouseY)?.let { message ->
                    val linkedMessage = message.linkedMessage
                    lastMovedToMessage = Pair(Pair(linkedMessage, message.wrappedIndex), Events.currentTick + 60)
                    val lineOffset = ChatManager.getLinesPerPageScaled() / 2 + 1 // center the message
                    findEnabled = false
                    ChatManager.selectedTab.refreshDisplayedMessage()
                    it.screen.rebuildWidgets0()
                    val displayIndex =
                        ChatManager.selectedTab.displayedMessages.indexOfFirst { line -> line.linkedMessage == linkedMessage }
                    val scrollTo = ChatManager.selectedTab.displayedMessages.size - displayIndex - lineOffset
                    ChatManager.selectedTab.scrollChat(scrollTo)
                }
            }
        }
        EventBus.register<ChatRenderPreLineAppearanceEvent>(10) {
            lastMovedToMessage?.let { message ->
                if (message.first.first != it.chatPlusGuiMessageLine.linkedMessage ||
                    message.first.second != it.chatPlusGuiMessageLine.wrappedIndex
                ) {
                    return@let
                }
                if (message.second < Events.currentTick) {
                    return@let
                }
                it.backgroundColor = findBackgroundColor
            }
        }
    }

    fun toggle(chatPlusScreen: ChatPlusScreen) {
        findEnabled = !findEnabled
        EventBus.post(FindToggleEvent(findEnabled))
        if (findEnabled) {
            ChatManager.selectedTab.refreshDisplayedMessage(chatPlusScreen.input?.value)
        } else {
            ChatManager.selectedTab.refreshDisplayedMessage()
        }
        chatPlusScreen.initial = chatPlusScreen.input!!.value
        chatPlusScreen.rebuildWidgets0()
    }

}