package com.ebicep.chatplus.features

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatTabAddNewMessageEvent
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ComponentContents
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import java.util.*

object CompactMessages {

    init {
        EventBus.register<ChatTabAddNewMessageEvent> {
            val chatTab = it.chatTab
            val messages = chatTab.messages
            val displayedMessages = chatTab.displayedMessages
            if (messages.isEmpty()) {
                return@register
            }
            val lastMessage = messages[messages.size - 1]
            val guiMessage = lastMessage.guiMessage
            val content = guiMessage.content.copy()
            content.siblings.removeIf { component -> component.contents is LiteralContentsIgnored }
            if (content.equals(it.componentWithTimeStamp)) {
                lastMessage.timesRepeated++
                guiMessage.content.siblings.removeIf { component -> component.contents is LiteralContentsIgnored }
                guiMessage.content.siblings.add(literalIgnored(" (${lastMessage.timesRepeated})").withStyle {
                    it.withColor(
                        ChatFormatting.GRAY
                    )
                })
                // remove previous displayed message and update it
                for (i in displayedMessages.size - 1 downTo 0) {
                    val displayedMessage = displayedMessages[i]
                    if (displayedMessage.linkedMessageIndex == messages.size - 1) {
                        displayedMessages.removeLast()
                    } else {
                        break
                    }
                }
                chatTab.addNewDisplayMessage(guiMessage.content as MutableComponent, it.addedTime, it.tag, it.linkedMessageIndex)
                it.returnFunction = true
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