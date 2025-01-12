package com.ebicep.chatplus.events

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.ConfigScreen
import com.ebicep.chatplus.config.queueUpdateConfig
import dev.architectury.event.CompoundEventResult
import dev.architectury.event.events.client.ClientGuiEvent
import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.client.ClientTickEvent

data class ChatPlusTickEvent(
    val tick: Long
) : Event

data class ChatPlusSecondEvent(
    val second: Long
) : Event

data class ChatPlusMinuteEvent(
    val minute: Long
) : Event

object Events {

    var currentTick = 1L

    init {
        ClientTickEvent.CLIENT_POST.register {
            EventBus.post(ChatPlusTickEvent(currentTick))
            if (currentTick % 20 == 0L) {
                EventBus.post(ChatPlusSecondEvent(currentTick / 20))
                if (currentTick % 1200 == 0L) {
                    EventBus.post(ChatPlusMinuteEvent(currentTick / 1200))
                }
            }

            ConfigScreen.handleOpenScreen()

            // save every 30s if there was a change or every 5 minutes
            if (currentTick % 600 == 0L && queueUpdateConfig || currentTick % 1800 == 0L) {
                queueUpdateConfig = false
                Config.save()
            }

            currentTick++
        }
        // scuffed fix for joining a server with a diff window dimension than before
        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register {
            Config.values.chatWindows.forEach { it.renderer.updateCachedDimension() }
        }
        ClientLifecycleEvent.CLIENT_STARTED.register {
            Config.values.chatWindows.forEach { it.renderer.updateCachedDimension() }
        }
        ClientLifecycleEvent.CLIENT_STOPPING.register {
            Config.save()
        }
        ClientGuiEvent.SET_SCREEN.register {
            return@register CompoundEventResult.pass()
        }
    }
}