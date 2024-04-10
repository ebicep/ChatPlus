package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatRenderPreLinesEvent
import com.ebicep.chatplus.util.KeyUtil.isDown

object PeakChat {

    init {
        EventBus.register<ChatRenderPreLinesEvent> {
            if (Config.values.keyPeekChat.isDown()) {
                it.chatFocused = true
            }
        }
    }

}