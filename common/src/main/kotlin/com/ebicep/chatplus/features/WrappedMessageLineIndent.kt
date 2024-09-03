package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatPositionTranslator
import com.ebicep.chatplus.features.chattabs.ChatTabGetMessageAtEvent
import com.ebicep.chatplus.features.chattabs.MessageAtType

object WrappedMessageLineIndent {

    init {
        EventBus.register<ChatTabGetMessageAtEvent> {
            val indent = Config.values.wrappedMessageLineIndent
            if (indent == 0) {
                return@register
            }
            if (it.messageAtType != MessageAtType.COMPONENT) {
                return@register
            }
            it.addMouseOperator { _, current ->
                val messageLine = ChatPositionTranslator.getMessageAtLineRelative(it.chatTab, current.x, current.y) ?: return@addMouseOperator
                if (messageLine.wrappedIndex == 0) {
                    return@addMouseOperator
                }
                current.x -= indent
            }
        }
    }

}