package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatScreenCloseEvent
import com.ebicep.chatplus.hud.ChatScreenInitPreEvent
import com.ebicep.chatplus.hud.ChatScreenSendMessagePostEvent
import com.ebicep.chatplus.mixin.IMixinChatScreen

object SaveInputBoxMessage {

    private var lastMessage: String = ""

    init {
        var saveLastMessage = true
        EventBus.register<ChatScreenInitPreEvent>({ 5 }) { // high priority so translate overrides
            if (!Config.values.saveInputBoxMessage) {
                return@register
            }
            val screen = it.screen as IMixinChatScreen
            if (screen.initial.startsWith("/")) {
                return@register
            }
            saveLastMessage = true
            screen.initial = lastMessage
        }
        EventBus.register<ChatScreenCloseEvent> {
            if (!saveLastMessage) {
                lastMessage = ""
            } else {
                val screen = it.screen as IMixinChatScreen
                lastMessage = screen.input.value
            }
        }
        EventBus.register<ChatScreenSendMessagePostEvent>({ 10 }) {
            saveLastMessage = false // dont save message if sent/consumed
        }
    }

}