package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatPlusScreen
import com.ebicep.chatplus.hud.ChatRenderPreLineAppearanceEvent

import com.ebicep.chatplus.hud.ChatScreenKeyPressedEvent
import com.ebicep.chatplus.util.TimeStampedLine
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft

object CopyMessage {

    init {
        var lastCopiedMessage: TimeStampedLine? = null
        var copiedMessageCooldown = -1L
        var messageCopied = false
        EventBus.register<ChatScreenKeyPressedEvent>(1, { messageCopied }) {
            ChatManager.selectedTab.getMessageAt(ChatPlusScreen.lastMouseX.toDouble(), ChatPlusScreen.lastMouseY.toDouble())
                ?.let { message ->
                    messageCopied = copiedMessageCooldown < Events.currentTick && Config.values.keyCopyMessageWithModifier.isDown()
                    if (!messageCopied) {
                        return@register
                    }
                    copiedMessageCooldown = Events.currentTick + 20
                    if (Config.values.copyNoFormatting) {
                        copyToClipboard(ChatFormatting.stripFormatting(message.content)!!)
                    } else {
                        copyToClipboard(message.content)
                    }
                    lastCopiedMessage = TimeStampedLine(message.line, Events.currentTick + 60)
                    //input!!.setEditable(false)
                    it.returnFunction = true
                }
        }
        EventBus.register<ChatRenderPreLineAppearanceEvent>(2) {
            if (lastCopiedMessage?.matches(it.line) == true) {
                it.backgroundColor = 402587903
            }
        }
    }

    private fun copyToClipboard(str: String) {
        Minecraft.getInstance().keyboardHandler.clipboard = str
    }

}