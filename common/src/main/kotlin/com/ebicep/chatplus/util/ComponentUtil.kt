package com.ebicep.chatplus.util

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.*
import net.minecraft.network.chat.contents.PlainTextContents
import java.awt.Color
import java.util.*


object ComponentUtil {


    fun literalIgnored(string: String, ignoredType: LiteralIgnoredType): MutableComponent {
        return MutableComponent.create(LiteralContentsIgnored(string, ignoredType))
    }

    class LiteralContentsIgnored(val text: String, private val ignoredType: LiteralIgnoredType) : PlainTextContents {

        override fun text(): String {
            return text
        }

        override fun <T> visit(arg: FormattedText.ContentConsumer<T>): Optional<T> {
            return arg.accept(this.text)
        }

        override fun <T> visit(arg: FormattedText.StyledContentConsumer<T>, arg2: Style): Optional<T> {
            return arg.accept(arg2, this.text)
        }

        override fun toString(): String {
            return "literalIgnored{" + this.text + "}"
        }

        override fun equals(other: Any?): Boolean {
            return if (this === other) {
                true
            } else if (other !is LiteralContentsIgnored) {
                false
            } else {
                this.text == other.text
            }
        }

        override fun hashCode(): Int {
            return text.hashCode()
        }

        fun isType(type: LiteralIgnoredType): Boolean {
            return ignoredType == type
        }

    }

    enum class LiteralIgnoredType {
        TIMESTAMP,
        COMPACT
    }


    fun componentIsType(component: Component, type: LiteralIgnoredType): Boolean {
        return component.contents is LiteralContentsIgnored && (component.contents as LiteralContentsIgnored).isType(type)
    }

    fun isTimestampContents(it: Component) = componentIsType(it, LiteralIgnoredType.TIMESTAMP)

    fun isCompactContents(it: Component) = componentIsType(it, LiteralIgnoredType.COMPACT)


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