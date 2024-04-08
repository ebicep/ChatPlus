package com.ebicep.chatplus

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatRenderPreLineAppearanceEvent
import com.ebicep.chatplus.hud.ChatRenderer
import com.ebicep.chatplus.util.KeyUtil.isDown

object PeakChat {

    init {
        EventBus.register<ChatRenderPreLineAppearanceEvent> {
            if (Config.values.keyPeekChat.isDown()) {
                it.textColor = (255.0 * 1.0 * ChatRenderer.textOpacity).toInt()
            }
        }
    }

}