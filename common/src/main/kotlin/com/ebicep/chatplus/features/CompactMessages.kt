package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.EnumTranslatableName
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.AddDisplayMessageType
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.ChatTabAddDisplayMessageEvent
import com.ebicep.chatplus.features.chattabs.ChatTabAddNewMessageEvent
import com.ebicep.chatplus.util.ComponentUtil
import com.ebicep.chatplus.util.ComponentUtil.isCompactContents
import com.ebicep.chatplus.util.ComponentUtil.isTimestampContents
import com.ebicep.chatplus.util.ComponentUtil.literalIgnored
import com.ebicep.chatplus.util.KotlinUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.util.Mth
import kotlin.math.max

object CompactMessages {

    private val COMPACT_STYLE = Style.EMPTY
        .withColor(TextColor.fromLegacyFormat(ChatFormatting.GRAY))
        .withBold(false)
        .withItalic(false)
        .withUnderlined(false)
        .withStrikethrough(false)
        .withObfuscated(false)

    init {
        EventBus.register<ChatTabAddNewMessageEvent> {
            if (!Config.values.compactMessagesEnabled) {
                return@register
            }
            val chatTab = it.chatTab
            val messages = chatTab.messages
            val displayedMessages = chatTab.displayedMessages
            if (messages.isEmpty()) {
                return@register
            }
            for (i in messages.size - 1 downTo max(0, messages.size - Config.values.compactMessagesSearchAmount)) {
                val message = messages[i]
                val guiMessage = message.guiMessage
                if (!componentEquals(guiMessage.content, it.mutableComponent)) {
                    continue
                }
                message.timesRepeated++
                // remove previous displayed message and update it
                var addIndex = -1
                var oldDisplayMessage: ChatTab.ChatPlusGuiMessageLine? = null
                for (j in displayedMessages.size - 1 downTo 0) {
                    val displayedMessage = displayedMessages[j]
                    if (messages[i] === displayedMessage.linkedMessage) {
                        displayedMessages.removeAt(j)
                        if (displayedMessage.wrappedIndex == 0) {
                            addIndex = j
                            oldDisplayMessage = displayedMessage
                            break
                        }
                    }
                }
                if (addIndex == -1 || oldDisplayMessage == null) {
                    break
                }
                it.mutableComponent.siblings.add(literalIgnored(" (${message.timesRepeated})", ComponentUtil.LiteralIgnoredType.COMPACT).withStyle(COMPACT_STYLE))
                val addedTime = if (Config.values.compactMessagesRefreshAddedTime) it.addedTime else oldDisplayMessage.line.addedTime
                val displayMessageEvent = EventBus.post(
                    ChatTabAddDisplayMessageEvent(
                        AddDisplayMessageType.COMPACT,
                        it.chatWindow,
                        chatTab,
                        it.mutableComponent,
                        addedTime,
                        oldDisplayMessage.line.tag,
                        message,
                        Mth.floor(it.chatWindow.renderer.getBackgroundWidth())
                    )
                )
                chatTab.addWrappedComponents(
                    it.mutableComponent,
                    displayMessageEvent,
                    addedTime,
                    oldDisplayMessage.line.tag,
                    message,
                    addIndex
                )
                it.returnFunction = true
                break
            }
        }
        EventBus.register<ChatTabAddDisplayMessageEvent> {
            if (it.addDisplayMessageType != AddDisplayMessageType.TAB) {
                return@register
            }
            if (it.linkedMessage.timesRepeated <= 1) {
                return@register
            }
            if (it.component.siblings.none { component -> component.contents is ComponentUtil.LiteralContentsIgnored }) {
                it.component.siblings.add(literalIgnored(" (${it.linkedMessage.timesRepeated})", ComponentUtil.LiteralIgnoredType.COMPACT).withStyle(COMPACT_STYLE))
            }
        }
    }

    enum class CompactComparatorMode(key: String) : EnumTranslatableName {
        VANILLA("chatPlus.compactMessages.comparatorMode.vanilla"),
        CUSTOM("chatPlus.compactMessages.comparatorMode.custom"),

        ;

        val translatable: Component = Component.translatable(key)

        override fun getTranslatableName(): Component {
            return translatable
        }
    }

    private fun componentEquals(component1: Component?, component2: Component?): Boolean {
        return when (Config.values.compactMessageComparatorMode) {
            CompactComparatorMode.VANILLA -> {
                var c1 = component1
                var c2 = component1
                if (component1 != null) {
                    c1 = c1!!.copy()
                    c1.siblings.removeIf { component -> isCompactContents(component) }
                }
                if (component2 != null) {
                    c2 = c2!!.copy()
                    c2.siblings.removeIf { component -> isCompactContents(component) }
                }
                return c1 == c2
            }

            CompactComparatorMode.CUSTOM -> Config.values.compactMessageSettings.componentEquals(component1, component2)
        }
    }

    @Serializable
    data class CompactMessageCustomSettings(
        var ignoreTimestamps: Boolean = true,

        var contents: Boolean = true,
        var style: Boolean = true,

        var styleSettings: CompactMessageCustomStyleSettings = CompactMessageCustomStyleSettings(),
    ) {

        @Transient
        private val filterComponents: (Component) -> Boolean = filterComponents@{
            if (isCompactContents(it)) {
                false
            } else if (ignoreTimestamps && isTimestampContents(it)) {
                false
            } else {
                true
            }
        }

        fun componentEquals(component1: Component?, component2: Component?): Boolean {
            if (component1 == null || component2 == null) {
                return component1 == component2
            }
            return (!contents || component1.contents == component2.contents) &&
                    (!style || componentStyleEquals(component1.style, component2.style)) &&
                    KotlinUtil.areListsEqual(component1.siblings.filter(filterComponents), component2.siblings.filter(filterComponents)) { c1, c2 -> componentEquals(c1, c2) }
        }

        private fun componentStyleEquals(style1: Style, style2: Style): Boolean {
            return (!styleSettings.color || style1.color == style2.color) &&
                    (!styleSettings.bold || style1.isBold == style2.isBold) &&
                    (!styleSettings.italic || style1.isItalic == style2.isItalic) &&
                    (!styleSettings.underlined || style1.isUnderlined == style2.isUnderlined) &&
                    (!styleSettings.strikethrough || style1.isStrikethrough == style2.isStrikethrough) &&
                    (!styleSettings.obfuscated || style1.isObfuscated == style2.isObfuscated) &&
                    (!styleSettings.clickEvent || style1.clickEvent == style2.clickEvent) &&
                    (!styleSettings.hoverEvent || componentHoverEquals(style1.hoverEvent, style2.hoverEvent)) &&
                    (!styleSettings.insertion || style1.insertion == style2.insertion) &&
                    (!styleSettings.font || style1.font == style2.font)
        }

        private fun componentHoverEquals(hoverEvent1: HoverEvent?, hoverEvent2: HoverEvent?): Boolean {
            if (hoverEvent1 == null || hoverEvent2 == null) {
                return hoverEvent1 == hoverEvent2
            }
            if (hoverEvent1.action == HoverEvent.Action.SHOW_TEXT && hoverEvent2.action == HoverEvent.Action.SHOW_TEXT) {
                val value1 = hoverEvent1.getValue(HoverEvent.Action.SHOW_TEXT)
                val value2 = hoverEvent2.getValue(HoverEvent.Action.SHOW_TEXT)
                return componentEquals(value1, value2)
            }
            return hoverEvent1 == hoverEvent2
        }

    }

    @Serializable
    data class CompactMessageCustomStyleSettings(
        var color: Boolean = true,
        var bold: Boolean = true,
        var italic: Boolean = true,
        var underlined: Boolean = true,
        var strikethrough: Boolean = true,
        var obfuscated: Boolean = true,
        var clickEvent: Boolean = true,
        var hoverEvent: Boolean = true,
        var insertion: Boolean = true,
        var font: Boolean = true,
    )


}