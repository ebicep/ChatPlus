package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatScreenInputBoxEditEvent
import com.ebicep.chatplus.mixin.IMixinChatScreen
import org.apache.commons.lang3.StringUtils

object InputBoxNormalizeInput {

    init {
        EventBus.register<ChatScreenInputBoxEditEvent> {
            if (Config.values.inputBoxSettings.normalizeInputWhileTyping) {
                val screen = it.screen as IMixinChatScreen
                val input = screen.input
                val endsWithSpace = input.value.endsWith(" ")
                val normalized = StringUtils.normalizeSpace(input.value) + if (endsWithSpace) " " else ""
                if (normalized != input.value) {
                    input.value = normalized
                }
            }
        }
    }

}