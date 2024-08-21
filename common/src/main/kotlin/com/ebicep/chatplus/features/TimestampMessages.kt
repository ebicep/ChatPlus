package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.TimestampMode
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.CompactMessages.literalIgnored
import com.ebicep.chatplus.features.chattabs.ChatTabAddNewMessageEvent
import com.ebicep.chatplus.util.KotlinUtil.containsReference
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TimestampMessages {

    init {
        EventBus.register<ChatTabAddNewMessageEvent>({ 5 }) {
            if (Config.values.chatTimestampMode == TimestampMode.NONE) {
                return@register
            }
            it.mutableComponent = getTimeStampedMessage(it.rawComponent)
        }
    }

    private fun getTimeStampedMessage(component: Component): MutableComponent {
        if (Config.values.chatTimestampMode == TimestampMode.NONE) {
            return component.copy() as MutableComponent
        }
        val componentWithTimeStamp: MutableComponent = Component.empty()
        val timestampedHoverComponents = HashSet<Any>()
        component.toFlatList().forEach {
            val flatComponent = it as MutableComponent
            if (flatComponent.style.hoverEvent == null) {
                flatComponent.withStyle {
                    val hoverValue = getTimestamp(false)
                    timestampedHoverComponents.add(hoverValue)
                    it.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverValue))
                }
            } else {
                when (flatComponent.style.hoverEvent?.action) {
                    HoverEvent.Action.SHOW_TEXT -> {
                        val hoverValue = flatComponent.style.hoverEvent?.getValue(HoverEvent.Action.SHOW_TEXT)
                        if (hoverValue != null && !timestampedHoverComponents.containsReference(hoverValue)) {
                            hoverValue.siblings.add(getTimestamp(true))
                            timestampedHoverComponents.add(hoverValue)
                        }
                    }

                    HoverEvent.Action.SHOW_ENTITY -> {
                        val hoverValue = flatComponent.style.hoverEvent?.getValue(HoverEvent.Action.SHOW_ENTITY)
                        if (hoverValue != null && !timestampedHoverComponents.containsReference(hoverValue.tooltipLines)) {
                            hoverValue.tooltipLines.add(getTimestamp(false))
                            timestampedHoverComponents.add(hoverValue.tooltipLines)
                        }
                    }
                }

            }
            componentWithTimeStamp.append(flatComponent)
        }
        return componentWithTimeStamp
    }

    private fun getTimestamp(newLine: Boolean): Component {
        return literalIgnored((if (newLine) "\n" else "") + "Sent at ")
            .withStyle {
                it.withColor(ChatFormatting.GRAY)
            }
            .append(Component.literal(getCurrentTime())
                .withStyle {
                    it.withColor(ChatFormatting.YELLOW)
                })
            .append(Component.literal("."))
    }

    private fun getCurrentTime(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(Config.values.chatTimestampMode.format))
    }


}