package com.ebicep.chatplus.events

import com.ebicep.chatplus.ChatPlus.isEnabled
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.ConfigScreen
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.hud.ChatPlusScreen
import dev.architectury.event.CompoundEventResult
import dev.architectury.event.events.client.ClientGuiEvent
import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.client.ClientTickEvent
import net.minecraft.client.gui.screens.ChatScreen


object Events {


    var latestDefaultText = ""
    var currentTick = 0L

    init {
        ClientTickEvent.CLIENT_POST.register {
            currentTick++

            ConfigScreen.handleOpenScreen()
            Config.values.chatTabs.forEach {
                if (it.resetDisplayMessageAtTick == currentTick) {
                    it.refreshDisplayedMessage()
                }
            }

            // save every 30s if there was a change or every 5 minutes
            if (currentTick % 600 == 0L && queueUpdateConfig || currentTick % 1800 == 0L) {
                queueUpdateConfig = false
                Config.save()
            }
        }
        ClientLifecycleEvent.CLIENT_STOPPING.register {
            Config.save()
        }
        ClientGuiEvent.SET_SCREEN.register {
            if (isEnabled() && it is ChatScreen) {
                return@register CompoundEventResult.interruptTrue(ChatPlusScreen(latestDefaultText))
            }
            return@register CompoundEventResult.pass()
        }
    }

}