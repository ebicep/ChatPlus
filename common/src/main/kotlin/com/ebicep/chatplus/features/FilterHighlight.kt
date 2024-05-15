package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.internal.MessageFilter
import com.ebicep.chatplus.hud.ChatRenderPreLineAppearanceEvent

import kotlinx.serialization.Serializable
import java.awt.Color

object FilterHighlight {

    val DEFAULT_COLOR = Color(0, 200, 0, 50).rgb

    init {
        EventBus.register<ChatRenderPreLineAppearanceEvent>({ 5 }) {
            if (!Config.values.filterHighlightEnabled) {
                return@register
            }
            val message = it.chatPlusGuiMessageLine.content
            for (filterHighlight in Config.values.filterHighlights) {
                if (filterHighlight.matches(message)) {
                    it.backgroundColor = filterHighlight.color
                }
            }
        }
    }

    @Serializable
    class Filter : MessageFilter {

        var color: Int = DEFAULT_COLOR

        constructor(pattern: String, color: Int) : super(pattern) {
            this.color = color
        }

    }

}