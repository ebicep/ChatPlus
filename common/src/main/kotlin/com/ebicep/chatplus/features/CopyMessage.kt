package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderPreLineAppearanceEvent
import com.ebicep.chatplus.hud.ChatScreenKeyPressedEvent
import com.ebicep.chatplus.util.TimeStampedLines
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft

object CopyMessage {

    init {
        var lastCopied: TimeStampedLines? = null
        var copiedMessageCooldown: Long = -1
        var messageCopied = false
        EventBus.register<ChatScreenKeyPressedEvent>({ 1 }, { messageCopied }) {
            val canCopyMessage = copiedMessageCooldown < Events.currentTick && Config.values.copyMessageKey.isDown()
            if (!canCopyMessage) {
                return@register
            }
            val copied: MutableSet<ChatTab.ChatPlusGuiMessageLine> = mutableSetOf()
            val hoveredOverMessage = ChatManager.selectedTab.getHoveredOverMessageLine()
            if (hoveredOverMessage != null && SelectChat.selectedMessages.isEmpty()) {
                copied.add(hoveredOverMessage)
                copyToClipboard(hoveredOverMessage)
            } else if (SelectChat.selectedMessages.isNotEmpty()) {
                copyToClipboard(SelectChat.selectedMessages.joinToString("\n") { line ->
                    copied.add(line)
                    line.content
                })
            }
            if (copied.isNotEmpty()) {
                messageCopied = true
                copiedMessageCooldown = Events.currentTick + 20
                lastCopied = TimeStampedLines(copied, Events.currentTick + 60)
                it.returnFunction = true
            }
        }
        EventBus.register<ChatRenderPreLineAppearanceEvent>({ Config.values.copyMessageLinePriority }) {
            if (lastCopied?.matches(it.line) == true) {
                it.backgroundColor = 402587903
            }
        }
    }

    private fun copyToClipboard(message: ChatTab.ChatPlusGuiMessageLine) {
        copyToClipboard(message.content)
    }

    private fun copyToClipboard(str: String) {
        if (Config.values.copyNoFormatting) {
            Minecraft.getInstance().keyboardHandler.clipboard = ChatFormatting.stripFormatting(str)!!
        } else {
            Minecraft.getInstance().keyboardHandler.clipboard = str
        }
    }

}