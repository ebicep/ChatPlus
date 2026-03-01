package com.ebicep.chatplus.util

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.mixin.IMixinStringSplitter
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.*
import net.minecraft.util.FormattedCharSequence
import java.awt.Color
import java.util.*


object ComponentUtil {


    fun literalIgnored(string: String, ignoredType: LiteralIgnoredType): MutableComponent {
        return MutableComponent.create(LiteralContentsIgnored(string, ignoredType))
    }

    class LiteralContentsIgnored(val text: String, private val ignoredType: LiteralIgnoredType) : ComponentContents {

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
        COMPACT,
        TRANSLATE
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

    fun splitLines(component: MutableComponent, maxWidth: Int? = 175): List<Component> {
        val lines = mutableListOf<Component>()
        component.visit({ _: Style, s: String ->
            Minecraft.getInstance().font.splitter.splitLines(s, maxWidth ?: 175, Style.EMPTY, false) { style: Style, pos: Int, len: Int ->
                lines.add(Component.literal(s.substring(pos, len)).withStyle(style))
            }
            Optional.empty<Any?>()
        }, Style.EMPTY)
        return lines
    }

    fun getWidthRange(
        sequence: FormattedCharSequence,
        originalString: String,
        substring: String,
    ): List<Pair<Float, Float>> {
        val widthProvider = (Minecraft.getInstance().font.splitter as IMixinStringSplitter).callGetWidthProvider()
        val substringLength = substring.length
        val originalString = ChatFormatting.stripFormatting(originalString)!!
        val startIndex = originalString.indexOf(substring, ignoreCase = Config.values.findMessageIgnoreCase)

        if (startIndex < 0 || startIndex + substringLength > originalString.length) {
            return mutableListOf<Pair<Float, Float>>()
        }

        var currentWidth = 0f
        var substringStartWidth: Float? = null
        var substringEndWidth: Float? = null
        var currentIndex = 0

        sequence.accept { _, style, codepoint ->
            if (currentIndex >= startIndex && currentIndex < startIndex + substringLength) {
                val charWidth = widthProvider.getWidth(codepoint, style)
                if (substringStartWidth == null) {
                    substringStartWidth = currentWidth
                }
                substringEndWidth = currentWidth + charWidth
            }

            currentWidth += widthProvider.getWidth(codepoint, style)
            currentIndex++

            // Stop early if we've processed the entire substring
            if (currentIndex >= startIndex + substringLength) {
                return@accept false
            }

            true
        }

        return if (substringStartWidth != null && substringEndWidth != null) {
            mutableListOf(Pair(substringStartWidth, substringEndWidth))
        } else {
            mutableListOf<Pair<Float, Float>>()
        }
    }

    fun getWidthRanges(
        sequence: FormattedCharSequence,
        originalString: String,
        regex: Regex
    ): List<Pair<Float, Float>> {
        val widthProvider = (Minecraft.getInstance().font.splitter as IMixinStringSplitter).callGetWidthProvider()
        val matches = regex.findAll(ChatFormatting.stripFormatting(originalString)!!).toList()

        if (matches.isEmpty()) {
            return emptyList()
        }

        val widthRanges = mutableListOf<Pair<Float, Float>>()
        var currentWidth = 0f
        var currentIndex = 0

        sequence.accept { _, style, codepoint ->
            for (match in matches) {
                if (currentIndex in match.range) {
                    val charWidth = widthProvider.getWidth(codepoint, style)
                    if (currentIndex == match.range.first) { // start of a match
                        widthRanges.add(Pair(currentWidth, currentWidth + charWidth))
                    } else if (currentIndex > match.range.first && currentIndex < match.range.last || currentIndex == match.range.last) { // middle or end of a match
                        widthRanges[widthRanges.lastIndex] = widthRanges.last().copy(second = currentWidth + charWidth)
                    }
                }
            }

            currentWidth += widthProvider.getWidth(codepoint, style)
            currentIndex++

            true
        }

        return widthRanges
    }

    private val START_COLOR_REGEX = Regex("^§[0-9A-FK-OR]", RegexOption.IGNORE_CASE)
    private val COLOR_REGEX = Regex("§([0-9A-FK-OR])", RegexOption.IGNORE_CASE)

//    fun FormattedText.getColoredString(): String {
//        var string = ""
//        var previousColor = ""
//        this.visit({ style: Style, str: String ->
//            val color = style.color
//            val colorString = if (color == null) {
//                "#ffffff"
//            } else {
//                "#${Integer.toHexString(color.value)}"
//            }
//            val text = formatString(str, colorString)
//            string += if (colorString == previousColor || START_COLOR_REGEX.containsMatchIn(str)) {
//                text
//            } else {
//                colorString + text
//            }
//            previousColor = colorString
//            Optional.empty<Any?>()
//        }, Style.EMPTY)
//        return string
//    }

    fun FormattedCharSequence.getColoredString(): String {
        var string = ""
        var previousStyleString = ""
        this.accept { index: Int, style: Style, code: Int ->
            var chr = code.toChar()
            if (style.isObfuscated) {
                chr = ' '
            }
            var styleString = style.getString()
            if (previousStyleString == styleString) {
                string += chr
            } else {
                string += styleString + chr
                previousStyleString = styleString
            }
            true
        }
        return string
    }

    fun FormattedCharSequence.getString(): String {
        var string = ""
        this.accept { index: Int, style: Style, code: Int ->
            var chr = code.toChar()
            if (style.isObfuscated) {
                chr = ' '
            }
            string += chr
            true
        }
        return string
    }

    fun Component.getColoredString(): String {
        return visualOrderText.getColoredString()
    }

    fun formatString(input: String, defaultColor: String): String {
        return COLOR_REGEX.replace(input) { matchResult ->
            val code = matchResult.groupValues[1]
            val chatFormatting = ChatFormatting.getByCode(code[0])
            if (chatFormatting != null && chatFormatting.color != null) {
                String.format("#%06X", chatFormatting.color) // hex color in #RRGGBB format
            } else if (code.equals("r", ignoreCase = true)) {
                defaultColor
            } else {
                ""
            }
        }
    }

    fun Style.getString(): String {
        var string = ""
        if (this.color != null) {
            string += "#${Integer.toHexString(this.color!!.value)}"
        }
        if (isBold) {
            string += "&l"
        }
        if (isItalic) {
            string += "&o"
        }
        if (isUnderlined) {
            string += "&n"
        }
        if (isStrikethrough) {
            string += "&m"
        }
        if (isObfuscated) {
            string += "&k"
        }
        return string
    }

}