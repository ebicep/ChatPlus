package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderPreLineAppearanceEvent
import com.ebicep.chatplus.hud.ChatScreenInputEvent
import com.ebicep.chatplus.util.ComponentUtil.getColoredString
import com.ebicep.chatplus.util.ComponentUtil.getString
import com.ebicep.chatplus.util.TimeStampedLines
import com.ebicep.chatplus.util.TimeStampedMessages
import com.ebicep.chatplus.util.Timestamped
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.util.FormattedCharSequence
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
                    copyToClipboard(getClipboardString(hoveredOverMessage.linkedMessage.guiMessage.content.visualOrderText))
                } else {
                    copiedLines.add(hoveredOverMessage)
                    copyToClipboard(getClipboardString(hoveredOverMessage))
                }
            } else if (selectedMessages.isNotEmpty()) {
                copyToClipboard(SelectChat.getSelectedMessagesOrdered().joinToString(Config.values.copyMessageSeparator) {
                    if (copyWholeMessage) {
                        copiedMessages.add(it.linkedMessage)
                        getClipboardString(it.linkedMessage.guiMessage.content.visualOrderText)
                    } else {
                        copiedLines.add(it)
                        getClipboardString(it)
                    }
                })
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

    private fun getClipboardString(message: ChatTab.ChatPlusGuiMessageLine): String {
        return if ((Config.values.copyNoFormatting && !Screen.hasShiftDown()) || (!Config.values.copyNoFormatting && Screen.hasShiftDown())) {
            message.content
        } else {
            if (Config.values.copyMessageFormattingSymbolOverride.isEmpty()) {
                message.coloredContent()
            } else {
                message.coloredContent().replace("&", Config.values.copyMessageFormattingSymbolOverride)
            }
        }
    }

    private fun getClipboardString(message: FormattedCharSequence): String {
        return if ((Config.values.copyNoFormatting && !Screen.hasShiftDown()) || (!Config.values.copyNoFormatting && Screen.hasShiftDown())) {
            message.getString()
        } else {
            if (Config.values.copyMessageFormattingSymbolOverride.isEmpty()) {
                message.getColoredString()
            } else {
                message.getColoredString().replace("&", Config.values.copyMessageFormattingSymbolOverride)
            }
        }
    }

    private fun copyToClipboard(message: String) {
        Minecraft.getInstance().keyboardHandler.clipboard = message
    }

}