package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatPlusScreen
import com.ebicep.chatplus.hud.ChatScreenKeyPressed
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

object CopyMessage {

    init {
        var messageCopied = false
        EventBus.register<ChatScreenKeyPressed>(1, { messageCopied }) {
            val window = Minecraft.getInstance().window.window
            val copyMessage = Config.values.keyCopyMessageWithModifier
            val copyMessageModifier = Config.values.keyCopyMessageWithModifier.modifier
            val copyMessageModifierDown = copyMessageModifier == 0.toShort() ||
                    (copyMessageModifier == 1.toShort() && Screen.hasAltDown()) ||
                    (copyMessageModifier == 2.toShort() && Screen.hasControlDown()) ||
                    (copyMessageModifier == 4.toShort() && Screen.hasShiftDown())
            messageCopied = ChatPlusScreen.copiedMessageCooldown < Events.currentTick &&
                    InputConstants.isKeyDown(window, copyMessage.key.value) &&
                    copyMessageModifierDown
            if (!messageCopied) {
                return@register
            }
            ChatPlusScreen.copiedMessageCooldown = Events.currentTick + 20
            ChatManager.selectedTab.getMessageAt(ChatPlusScreen.lastMouseX.toDouble(), ChatPlusScreen.lastMouseY.toDouble())?.let {
                if (Config.values.copyNoFormatting) {
                    copyToClipboard(ChatFormatting.stripFormatting(it.content)!!)
                } else {
                    copyToClipboard(it.content)
                }
                ChatPlusScreen.lastCopiedMessage = Pair(it.line, Events.currentTick + 60)
                //input!!.setEditable(false)
            }
            it.returnKeyPressed = true
        }
    }

    private fun copyToClipboard(str: String) {
        Minecraft.getInstance().keyboardHandler.clipboard = str
    }

}