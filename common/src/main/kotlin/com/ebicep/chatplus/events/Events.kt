package com.ebicep.chatplus.events

import com.ebicep.chatplus.ChatPlus.isEnabled
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatPlusScreen
import dev.architectury.event.CompoundEventResult
import dev.architectury.event.events.client.ClientGuiEvent
import dev.architectury.event.events.client.ClientTickEvent
import net.minecraft.client.gui.screens.ChatScreen


object Events {


    var latestDefaultText = ""
    var currentTick = 0L

    init {
        ClientTickEvent.CLIENT_POST.register {
            currentTick++

            ChatManager.chatTabs.forEach {
                if (it.resetDisplayMessageAtTick == currentTick) {
                    it.refreshDisplayedMessage()
                }
            }
        }
        ClientGuiEvent.SET_SCREEN.register {
            if (isEnabled() && it is ChatScreen) {
                return@register CompoundEventResult.interruptTrue(ChatPlusScreen(latestDefaultText))
            }
            return@register CompoundEventResult.pass()
        }
    }

}