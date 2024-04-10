package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatRenderPreLineAppearanceEvent

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

object FilterHighlight {

    const val DEFAULT_COLOR = -16737281

    init {
        EventBus.register<ChatRenderPreLineAppearanceEvent>(5) {
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
    class Filter {

        var pattern: String
            set(value) {
                field = value
                regex = Regex(value)
            }
        var color: Int = DEFAULT_COLOR

        constructor(pattern: String, color: Int) {
            this.pattern = pattern
            this.color = color
            this.regex = Regex(pattern)
        }

        @Transient
        var regex: Regex = Regex("")

        fun matches(message: String): Boolean {
            return regex.matches(message)
        }

    }

}