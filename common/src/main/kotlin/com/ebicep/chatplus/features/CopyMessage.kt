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
        var copiedMessageCooldown = -1L
        var messageCopied = false
        EventBus.register<ChatScreenKeyPressedEvent>(1, { messageCopied }) {
            ChatManager.selectedTab.getHoveredOverMessage()?.let { message ->
                messageCopied = copiedMessageCooldown < Events.currentTick && Config.values.keyCopyMessageWithModifier.isDown()
                if (!messageCopied) {
                    return@register
                }
                copiedMessageCooldown = Events.currentTick + 20
                val copied: MutableSet<ChatTab.ChatPlusGuiMessageLine> = mutableSetOf()
                if (SelectChat.selectedMessages.isEmpty()) {
                    copied.add(message)
                    copyToClipboard(message)
                } else {
                    copyToClipboard(SelectChat.selectedMessages.joinToString("\n") {
                        copied.add(it)
                        it.content
                    })
                }
                lastCopied = TimeStampedLines(copied, Events.currentTick + 60)
                it.returnFunction = true
            }
        }
        EventBus.register<ChatRenderPreLineAppearanceEvent>(2) {
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