package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatRenderPreLinesEvent
import com.ebicep.chatplus.util.KeyUtil.isDown

object PeekChat {

    val peeking: Boolean
        get() = Config.values.keyPeekChat.isDown()

    init {
        var wasPeeking = false
        EventBus.register<ChatRenderPreLinesEvent> {
            if (peeking) {
                wasPeeking = true
                it.chatFocused = true
            } else if (wasPeeking) {
                wasPeeking = false
                it.chatWindow.tabSettings.selectedTab.resetChatScroll()
            }
        }
    }

}