package com.ebicep.chatplus.features

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chattabs.ChatTabAddDisplayMessageEvent
import com.ebicep.chatplus.features.textbarelements.FindTextBarElement
import com.ebicep.chatplus.features.textbarelements.TextBarElements
import com.ebicep.chatplus.features.textbarelements.TranslateToggleEvent
import com.ebicep.chatplus.hud.*
import net.minecraft.client.Minecraft
import java.awt.Color

object FindText {

    const val FIND_COLOR = 0xFFFFFF55
    private val findBackgroundColor = Color(FIND_COLOR.toInt()).darker().rgb
    var findEnabled: Boolean = false

    init {
        var lastMovedToMessage: Pair<Pair<Int, Int>, Long>? = null // <linked index, wrapped index>, tick
        EventBus.register<TextBarElements.AddTextBarElementEvent> {
            it.elements.add(FindTextBarElement(it.screen))
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
                    lastMovedToMessage = Pair(Pair(message.linkedMessageIndex, message.wrappedIndex), Events.currentTick + 60)
                    val lineOffset = ChatManager.getLinesPerPage() / 3
                    val scrollTo = ChatManager.selectedTab.messages.size - message.linkedMessageIndex - lineOffset
                    findEnabled = false
                    ChatManager.selectedTab.refreshDisplayedMessage()
                    it.screen.rebuildWidgets0()
                    ChatManager.selectedTab.scrollChat(scrollTo)
                }
            }
        }
        EventBus.register<ChatRenderLineBackgroundEvent>(10) {
            lastMovedToMessage?.let { message ->
                if (message.first.first != it.chatPlusGuiMessageLine.linkedMessageIndex || message.first.second != it
                        .chatPlusGuiMessageLine.wrappedIndex
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

}