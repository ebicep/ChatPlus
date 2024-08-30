package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chattabs.*
import com.ebicep.chatplus.features.textbarelements.FindTextBarElement
import com.ebicep.chatplus.features.textbarelements.FindToggleEvent
import com.ebicep.chatplus.features.textbarelements.TextBarElements
import com.ebicep.chatplus.features.textbarelements.TranslateToggleEvent
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.mixin.IMixinScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import java.awt.Color

object FindMessage {

    val FIND_COLOR = Color(255, 255, 85, 255).rgb
    private val findBackgroundColor = Color(FIND_COLOR).darker().rgb
    var findEnabled: Boolean = false
    private var lastInput = ""

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
            if (findEnabled) {
                findEnabled = false
                ChatManager.globalSelectedTab.resetFilter()
            }
        }
        EventBus.register<ChatTabRewrapDisplayMessages> {
            findEnabled = false
            ChatManager.globalSelectedTab.resetFilter()
        }
        EventBus.register<ChatTabRefreshDisplayMessages> {
            if (findEnabled && lastInput.isNotEmpty()) {
                it.predicates.add { guiMessage ->
                    guiMessage.guiMessage.content.string.contains(lastInput, ignoreCase = true)
                }
            }
        }
        EventBus.register<ChatTabSwitchEvent> {
            if (findEnabled) {
                ChatManager.globalSelectedTab.queueRefreshDisplayedMessages(false)
            }
        }
        EventBus.register<ChatTabAddDisplayMessageEvent> {
            val screen = Minecraft.getInstance().screen
            if (findEnabled && screen is IMixinChatScreen) {
                it.filtered = true
                val filter = screen.input?.value
                if (filter != null && !it.component.string.contains(filter, ignoreCase = true)) {
                    it.addMessage = false
                }
            }
        }
        EventBus.register<ChatScreenInputBoxEditEvent> {
            if (findEnabled) {
                lastInput = it.str
                ChatManager.globalSelectedTab.queueRefreshDisplayedMessages(false)
                it.returnFunction = true
            }
        }
        EventBus.register<ChatScreenRenderEvent> {
            if (findEnabled && Config.values.findMessageHighlightInputBox) {
                it.screen as IMixinChatScreen
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
                ChatManager.globalSelectedTab.resetFilter()
            }
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            if (it.button != 0) {
                return@register
            }
            if (findEnabled) {
                ChatManager.globalSelectedTab.getHoveredOverMessageLine(it.mouseX, it.mouseY)?.let { message ->
                    val linkedMessage = message.linkedMessage
                    lastMovedToMessage = Pair(Pair(linkedMessage, message.wrappedIndex), Events.currentTick + 60)
                    findEnabled = false
                    ChatManager.globalSelectedTab.moveToMessage(it.screen, message)
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

    fun toggle(chatPlusScreen: ChatScreen) {
        chatPlusScreen as IMixinChatScreen
        findEnabled = !findEnabled
        EventBus.post(FindToggleEvent(findEnabled))
        chatPlusScreen.initial = chatPlusScreen.input!!.value
        lastInput = chatPlusScreen.input?.value ?: ""
        if (!findEnabled) {
            ChatManager.globalSelectedTab.resetFilter()
        } else {
            ChatManager.globalSelectedTab.queueRefreshDisplayedMessages(false)
        }
        chatPlusScreen as IMixinScreen
        chatPlusScreen.callRebuildWidgets()
    }

}