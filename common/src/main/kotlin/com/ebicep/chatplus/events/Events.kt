package com.ebicep.chatplus.events

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.ChatPlus.isEnabled
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.ConfigScreen
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatPlusScreen
import com.ebicep.chatplus.hud.ChatRenderer
import dev.architectury.event.CompoundEventResult
import dev.architectury.event.events.client.ClientGuiEvent
import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.client.ClientTickEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen

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

    var latestDefaultText = ""
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

            val messagesToSend = ChatPlusScreen.messagesToSend
            if (messagesToSend.isNotEmpty() && ChatPlusScreen.lastMessageSentTick + 10 < currentTick) {
                val message = messagesToSend.removeAt(0)
                ChatManager.addSentMessage(message)
                try {
                    Minecraft.getInstance().player!!.connection.sendChat(message)
                } catch (e: Exception) {
                    ChatPlus.LOGGER.error(e)
                }
            }

            // save every 30s if there was a change or every 5 minutes
            if (currentTick % 600 == 0L && queueUpdateConfig || currentTick % 1800 == 0L) {
                queueUpdateConfig = false
                Config.save()
            }

            currentTick++
        }
        // scuffed fix for joining a server with a diff window dimension than before
        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register {
            ChatRenderer.updateCachedDimension()
        }
        ClientLifecycleEvent.CLIENT_STARTED.register {
            ChatRenderer.updateCachedDimension()
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