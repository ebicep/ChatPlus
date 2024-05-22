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

object FindMessage {

    val FIND_COLOR = Color(255, 255, 85, 255).rgb
    private val findBackgroundColor = Color(FIND_COLOR).darker().rgb
    var findEnabled: Boolean = false

    init {
        var lastMovedToMessage: Pair<Pair<ChatTab.ChatPlusGuiMessage, Int>, Long>? = null // <linked message, wrapped index>, tick
        EventBus.register<TextBarElements.AddTextBarElementEvent>({ 100 }) {
            if (!Config.values.findMessageEnabled) {
                return@register
            }
            if (Config.values.findMessageTextBarElementEnabled) {
                it.elements.add(FindTextBarElement(it.screen))
            }
        }
        var findShortcutUsed = false
        EventBus.register<ChatScreenKeyPressedEvent>({ 1 }, { findShortcutUsed }) {
            if (!Config.values.findMessageEnabled) {
                return@register
            }
            findShortcutUsed = Config.values.findMessageKey.isDown()
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
                ChatManager.selectedTab.refreshDisplayedMessage { guiMessage ->
                    guiMessage.guiMessage.content.string.contains(it.str, ignoreCase = true)
                }
                it.returnFunction = true
            }
        }
        EventBus.register<ChatScreenRenderEvent> {
            if (findEnabled && Config.values.findMessageHighlightInputBox) {
                val editBox = it.screen.input ?: return@register
                it.guiGraphics.renderOutline(
                    editBox.x - 2,
                    editBox.y - 5,
                    editBox.width - 1,
                    editBox.height,
                    FIND_COLOR
                )
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
                if (filter != null && !it.component.string.contains(filter, ignoreCase = true)) {
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
                    findEnabled = false
                    ChatManager.selectedTab.moveToMessage(it.screen, message)
                }
            }
        }
        EventBus.register<ChatRenderPreLineAppearanceEvent>({ Config.values.findMessageLinePriority }) {
            lastMovedToMessage?.let { message ->
                if (message.first.first !== it.chatPlusGuiMessageLine.linkedMessage ||
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
            ChatManager.selectedTab.refreshDisplayedMessage { guiMessage ->
                val value = chatPlusScreen.input?.value ?: return@refreshDisplayedMessage false
                guiMessage.guiMessage.content.string.contains(value, ignoreCase = true)
            }
        } else {
            ChatManager.selectedTab.refreshDisplayedMessage()
        }
        chatPlusScreen.initial = chatPlusScreen.input!!.value
        chatPlusScreen.rebuildWidgets0()
    }

}