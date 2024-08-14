package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.ChatTabAddDisplayMessageEvent
import com.ebicep.chatplus.features.chattabs.ChatTabAddNewMessageEvent
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.*
import net.minecraft.util.Mth
import java.util.*
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
                if (Config.values.compactMessagesIgnoreTimestamps) {
                    if (message.rawComponent != it.component) {
                        continue
                    }
                } else {
                    val content = guiMessage.content.copy()
                    content.siblings.removeIf { component -> component.contents is LiteralContentsIgnored } // remove repeated component
                    if (!content.equals(it.componentWithTimeStamp)) {
                        continue
                    }
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
                it.componentWithTimeStamp.siblings.add(literalIgnored(" (${message.timesRepeated})").withStyle(COMPACT_STYLE))
                val addedTime = if (Config.values.compactMessagesRefreshAddedTime) it.addedTime else oldDisplayMessage.line.addedTime
                val displayMessageEvent = EventBus.post(
                    ChatTabAddDisplayMessageEvent(
                        it.chatWindow,
                        chatTab,
                        it.componentWithTimeStamp,
                        addedTime,
                        oldDisplayMessage.line.tag,
                        message,
                        Mth.floor(it.chatWindow.renderer.getBackgroundWidth())
                    )
                )
                chatTab.addWrappedComponents(
                    it.componentWithTimeStamp,
                    displayMessageEvent,
                    addedTime,
                    oldDisplayMessage.line.tag,
                    message,
                    addIndex
                )
                it.returnFunction = true
                break
            }
            // save memory, remove raw component if it's not needed
            val removeRawComponentStartIndex = max(0, messages.size - Config.values.compactMessagesSearchAmount - 1)
            for (i in removeRawComponentStartIndex downTo max(0, removeRawComponentStartIndex - 25)) {
                messages[i].rawComponent = null
            }
        }
    }

    fun literalIgnored(string: String): MutableComponent {
        return MutableComponent.create(LiteralContentsIgnored(string))
    }

    class LiteralContentsIgnored(val text: String) : ComponentContents {

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

    }


}