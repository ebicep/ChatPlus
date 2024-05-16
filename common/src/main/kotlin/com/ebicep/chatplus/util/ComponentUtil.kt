package com.ebicep.chatplus.util

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent


object ComponentUtil {

    fun literal(text: String, color: ChatFormatting, hoverEvent: HoverEvent? = null): MutableComponent {
        return Component.literal(text).withStyle {
            it.withColor(color).withHoverEvent(hoverEvent)
        }
    }

    fun translatable(text: String, color: ChatFormatting, hoverEvent: HoverEvent? = null): MutableComponent {
        return Component.translatable(text).withStyle {
            it.withColor(color).withHoverEvent(hoverEvent)
        }
    }

    fun MutableComponent.append(text: String, color: ChatFormatting): MutableComponent {
        return this.append(Component.literal(text).withStyle(color))
    }

}