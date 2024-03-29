package com.ebicep.chatplus.events

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.ChatPlus.isEnabled
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.ConfigScreen
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatPlusScreen
import com.ebicep.chatplus.hud.ChatRenderer
import com.ebicep.chatplus.translator.Translator
import com.ebicep.chatplus.translator.languageTo
import dev.architectury.event.CompoundEventResult
import dev.architectury.event.events.client.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Component

data class ChatPlusTickEvent(
    val tick: Long
) : Event

object Events {

    var latestDefaultText = ""
    var currentTick = 1L

    init {
        ClientTickEvent.CLIENT_POST.register {
            EventBus.post(ChatPlusTickEvent(currentTick))

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

        ClientChatEvent.RECEIVED.register { type: ChatType.Bound, component: Component ->
            handleTranslate(component)
            CompoundEventResult.pass()
        }
        ClientSystemMessageEvent.RECEIVED.register { component: Component ->
            handleTranslate(component)
            CompoundEventResult.pass()
        }
    }

    private fun handleTranslate(component: Component) {
        if (!Config.values.translatorEnabled) {
            return
        }
        val unformattedText = component.string
        languageTo?.let {
            Translator(unformattedText, null, it).start()
        }
    }

}