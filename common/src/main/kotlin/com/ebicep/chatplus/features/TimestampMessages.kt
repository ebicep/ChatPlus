package com.ebicep.chatplus.features

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.EnumTranslatableName
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.AddNewMessageEvent
import com.ebicep.chatplus.util.ComponentUtil
import com.ebicep.chatplus.util.ComponentUtil.literalIgnored
import com.ebicep.chatplus.util.KotlinUtil.containsReference
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TimestampMessages {

    private var lastTime: String = ""
    private var lastTimestamp: Component = Component.empty()
    private var lastTimestampNewLine: Component = Component.empty()

    init {
        EventBus.register<AddNewMessageEvent>({ 5 }) {
            if (!Config.values.timestampSettings.enabled) {
                it.mutableComponent = it.rawComponent.copy()
                return@register
            }
            val currentTime = getCurrentTime()
            if (lastTime != currentTime) {
                lastTime = currentTime
                lastTimestamp = getTimestamp(false)
                lastTimestampNewLine = getTimestamp(true)
            }
            try {
                it.mutableComponent = getTimeStampedMessage(it.rawComponent)
            } catch (e: Exception) {
                ChatPlus.LOGGER.error(e)
                it.mutableComponent = it.rawComponent.copy()
            }
        }
    }

    private fun getTimeStampedMessage(component: Component): MutableComponent {
        val componentWithTimeStamp: MutableComponent = Component.empty()
        return when (Config.values.timestampSettings.chatTimestampModeType) {
            TimestampModeType.HOVER -> {
                val timestampedHoverComponents = HashSet<Any>()
                component.toFlatList().forEach {
                    val flatComponent = it as MutableComponent
                    if (flatComponent.style.hoverEvent == null) {
                        flatComponent.withStyle {
                            timestampedHoverComponents.add(lastTimestamp)
                            it.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, lastTimestamp))
                        }
                    } else {
                        when (flatComponent.style.hoverEvent?.action) {
                            HoverEvent.Action.SHOW_TEXT -> {
                                val hoverValue = flatComponent.style.hoverEvent?.getValue(HoverEvent.Action.SHOW_TEXT)
                                if (hoverValue != null && !timestampedHoverComponents.containsReference(hoverValue)) {
                                    hoverValue.siblings.add(lastTimestampNewLine)
                                    timestampedHoverComponents.add(hoverValue)
                                }
                            }

                            HoverEvent.Action.SHOW_ENTITY -> {
                                val hoverValue = flatComponent.style.hoverEvent?.getValue(HoverEvent.Action.SHOW_ENTITY)
                                if (hoverValue != null && !timestampedHoverComponents.containsReference(hoverValue.tooltipLines)) {
                                    hoverValue.tooltipLines.add(lastTimestamp)
                                    timestampedHoverComponents.add(hoverValue.tooltipLines)
                                }
                            }
                        }

                    }
                    componentWithTimeStamp.append(flatComponent)
                }
                return componentWithTimeStamp
            }

            TimestampModeType.PREPEND -> {
                componentWithTimeStamp.append(lastTimestamp)
                componentWithTimeStamp.append(component)
                return componentWithTimeStamp
            }

            TimestampModeType.APPEND -> {
                componentWithTimeStamp.append(component)
                componentWithTimeStamp.append(lastTimestamp)
                return componentWithTimeStamp
            }

        }
    }

    private fun getTimestamp(newLine: Boolean): Component {
        return literalIgnored((if (newLine) "\n" else "") + getCurrentTime(), ComponentUtil.LiteralIgnoredType.TIMESTAMP)
    }

    private fun getCurrentTime(): String {
        val format = Config.values.timestampSettings.timestampFormat.replace("&", "§")
        val start = format.lastIndexOf('{')
        val end = format.indexOf('}')
        if (start == -1 || end == -1 || start >= end) {
            return "Invalid Time Format"
        }
        val time = format.substring(start + 1, end)
        val formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern(time))
        return format.substring(0, start) + formattedTime + format.substring(end + 1)
    }

    @Serializable
    data class TimestampSettings(
        var enabled: Boolean = true,
        var timestampFormat: String = "&7Sent at &e{hh:mm:ss a}&7.",
        var chatTimestampModeType: TimestampModeType = TimestampModeType.HOVER,
    )

    @Serializable
    enum class TimestampModeType(key: String) : EnumTranslatableName {
        HOVER("chatPlus.chatSettings.chatTimestampModeType.hover"),
        PREPEND("chatPlus.chatSettings.chatTimestampModeType.prepend"),
        APPEND("chatPlus.chatSettings.chatTimestampModeType.append"),

        ;

        val translatable: Component = Component.translatable(key)

        override fun getTranslatableName(): Component {
            return translatable
        }

    }

}
