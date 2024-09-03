package com.ebicep.chatplus.util

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import java.awt.Color
import java.util.*


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

    fun MutableComponent.withColor(color: Int, alpha: Boolean = false): MutableComponent {
        return this.withStyle { it.withColor(Color(color, alpha).rgb) }
    }

    fun splitLines(component: MutableComponent, maxWidth: Int = 175): List<Component> {
        val lines = mutableListOf<Component>()
        component.visit({ _: Style, s: String ->
            Minecraft.getInstance().font.splitter.splitLines(s, maxWidth, Style.EMPTY, false) { style: Style, pos: Int, len: Int ->
                lines.add(Component.literal(s.substring(pos, len)).withStyle(style))
            }
            Optional.empty<Any?>()
        }, Style.EMPTY)
        return lines
    }

}