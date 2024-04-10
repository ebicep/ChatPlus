package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderPreLineAppearanceEvent

import com.ebicep.chatplus.hud.ChatScreenCloseEvent
import com.ebicep.chatplus.hud.ChatScreenRenderEvent
import net.minecraft.client.GuiMessage

object HoverHighlight {

    data class HoverHighlightRenderEvent(
        val line: GuiMessage.Line,
        var cancelled: Boolean = false
    ) : Event

    init {
        var hoveredOverMessage: GuiMessage.Line? = null
        EventBus.register<ChatScreenRenderEvent> {
            if (!Config.values.hoverHighlightEnabled) {
                return@register
            }
            hoveredOverMessage = ChatManager.selectedTab.getMessageAt(it.mouseX.toDouble(), it.mouseY.toDouble())?.line
        }
        EventBus.register<ChatRenderPreLineAppearanceEvent>(15) {
            val hoveredOver = it.line === hoveredOverMessage
            if (hoveredOver) {
                val renderEvent = EventBus.post(HoverHighlightRenderEvent(it.line))
                if (renderEvent.cancelled) {
                    return@register
                }
                it.backgroundColor = Config.values.hoverHighlightColor
            }
        }
        EventBus.register<ChatScreenCloseEvent> {
            hoveredOverMessage = null
        }
    }

}