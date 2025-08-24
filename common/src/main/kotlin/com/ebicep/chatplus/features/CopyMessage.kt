package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderPreLineAppearanceEvent
import com.ebicep.chatplus.hud.ChatScreenInputEvent
import com.ebicep.chatplus.util.TimeStampedLines
import com.ebicep.chatplus.util.TimeStampedMessages
import com.ebicep.chatplus.util.Timestamped
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import java.awt.Color

object CopyMessage {

    val DEFAULT_COLOR = Color(255, 0, 255, 255).rgb

    init {
        var lastCopied: Timestamped? = null
        var copiedMessageCooldown: Long = -1
        var messageCopied = false
        EventBus.register<ChatScreenInputEvent>({ 1 }, { messageCopied }) {
            val canCopyMessage = copiedMessageCooldown < Events.currentTick && Config.values.copyMessageKey.isDown()
            if (!canCopyMessage) {
                messageCopied = false
                return@register
            }
            val copiedLines: MutableList<ChatTab.ChatPlusGuiMessageLine> = mutableListOf()
            val copiedMessages: MutableList<ChatTab.ChatPlusGuiMessage> = mutableListOf()
            val hoveredOverMessage = ChatManager.globalSelectedTab.getHoveredOverMessageLine()
            val selectedMessages = SelectChat.getAllSelectedMessages()
            val copyWholeMessage = Config.values.copyWholeMessage
            if (hoveredOverMessage != null && selectedMessages.isEmpty()) {
                if (copyWholeMessage) {
                    copiedMessages.add(hoveredOverMessage.linkedMessage)
                    copyToClipboard(hoveredOverMessage.linkedMessage.guiMessage.content.string)
                } else {
                    copiedLines.add(hoveredOverMessage)
                    copyToClipboard(hoveredOverMessage)
                }
            } else if (selectedMessages.isNotEmpty()) {
                if (copyWholeMessage) {
                    copyToClipboard(SelectChat.getSelectedMessagesOrdered().map { it.linkedMessage }.joinToString(Config.values.copyMessageSeparator) { message ->
                        copiedMessages.add(message)
                        message.guiMessage.content.string
                    })
                } else {
                    copyToClipboard(SelectChat.getSelectedMessagesOrdered().joinToString(Config.values.copyMessageSeparator) { line ->
                        copiedLines.add(line)
                        line.content
                    })
                }
            }
            if (copyWholeMessage && copiedMessages.isNotEmpty() || !copyWholeMessage && copiedLines.isNotEmpty()) {
                messageCopied = true
                copiedMessageCooldown = Events.currentTick + 20
                lastCopied = if (copyWholeMessage) {
                    TimeStampedMessages(copiedMessages, Events.currentTick + 60)
                } else {
                    TimeStampedLines(copiedLines, Events.currentTick + 60)
                }
                it.returnFunction = true
            }
        }
        EventBus.register<ChatRenderPreLineAppearanceEvent>({ Config.values.copyMessageLinePriority }) {
            if (lastCopied?.matches(it) == true) {
                it.backgroundColor = 402587903
            }
        }
    }

    private fun copyToClipboard(message: ChatTab.ChatPlusGuiMessageLine) {
        copyToClipboard(message.content)
    }

    private fun copyToClipboard(str: String) {
        val keyboardHandler = Minecraft.getInstance().keyboardHandler
        if ((Config.values.copyNoFormatting && !Screen.hasShiftDown()) || (!Config.values.copyNoFormatting && Screen.hasShiftDown())) {
            keyboardHandler.clipboard = ChatFormatting.stripFormatting(str)!!
        } else {
            if (Config.values.copyMessageFormattingSymbolOverride.isEmpty()) {
                keyboardHandler.clipboard = str
            } else {
                keyboardHandler.clipboard = str.replace("§", Config.values.copyMessageFormattingSymbolOverride)
            }
        }
    }

}