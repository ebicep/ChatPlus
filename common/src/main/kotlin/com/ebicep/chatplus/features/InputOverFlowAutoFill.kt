package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.EnumTranslatableName
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatScreenCloseEvent
import com.ebicep.chatplus.hud.ChatScreenInitPostEvent
import com.ebicep.chatplus.hud.ChatScreenKeyPressedEvent
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import java.util.*

object InputOverFlowAutoFill {

    var messagesToSend: Deque<String> = LinkedList()
    var updated = false // handle when initially sending message that adds to queue (dont clear queue)

    init {
        var skipped = false
        var lastKey = 0
        EventBus.register<ChatScreenInitPostEvent> {
            if (!Config.values.inputOverFlowAutoFillSettings.enabled) {
                return@register
            }
            if (messagesToSend.isEmpty()) {
                return@register
            }
            it.screen as IMixinChatScreen
            val command = it.screen.initial == "/"
            if (command && Config.values.inputOverFlowAutoFillSettings.autoFillCommandInteraction == AutoFillCommandInteraction.SKIP) {
                skipped = true
                return@register
            } else if (command && Config.values.inputOverFlowAutoFillSettings.autoFillCommandInteraction == AutoFillCommandInteraction.CLEAR) {
                messagesToSend.clear()
            } else {
                skipped = false
                updated = false
                it.screen.input.value = messagesToSend.peekFirst()
            }
        }
        EventBus.register<ChatScreenKeyPressedEvent> {
            if (!Config.values.inputOverFlowAutoFillSettings.enabled) {
                return@register
            }
            lastKey = it.keyCode
        }
        EventBus.register<ChatScreenCloseEvent> {
            if (!Config.values.inputOverFlowAutoFillSettings.enabled) {
                return@register
            }
            if (skipped) {
                return@register
            }
            if (updated) {
                return@register
            }
            if (messagesToSend.isEmpty()) {
                return@register
            }
            it.screen as IMixinChatScreen
            if (it.screen.input.value.isEmpty() && Config.values.inputOverFlowAutoFillSettings.clearQueueIfCloseOnEmpty) {
                messagesToSend.clear()
                return@register
            }
            if (Config.values.inputOverFlowAutoFillSettings.onlyOnEnter) {
                if (lastKey == InputConstants.KEY_RETURN || lastKey == InputConstants.KEY_NUMPADENTER) {
                    messagesToSend.removeFirst()
                }
            } else {
                messagesToSend.removeFirst()
            }
        }
    }

    fun addToQueue(messages: List<String>) {
        if (!Config.values.inputOverFlowAutoFillSettings.enabled) {
            return
        }
        if (messages.isEmpty()) {
            return
        }
        when (Config.values.inputOverFlowAutoFillSettings.queueMode) {
            QueueMode.OVERWRITE -> messagesToSend = LinkedList(messages)
            QueueMode.PREPEND -> messages.reversed().forEach { messagesToSend.addFirst(it) }
            QueueMode.APPEND -> messages.forEach { messagesToSend.addLast(it) }
        }
        updated = true
    }

    @Serializable
    data class InputOverFlowAutoFillSettings(
        var enabled: Boolean = true,
        var onlyOnEnter: Boolean = true,
        var clearQueueIfCloseOnEmpty: Boolean = true,
        var autoFillCommandInteraction: AutoFillCommandInteraction = AutoFillCommandInteraction.IGNORE,
        var queueMode: QueueMode = QueueMode.OVERWRITE,
    )


    @Serializable
    enum class AutoFillCommandInteraction(key: String) : EnumTranslatableName {
        IGNORE("chatPlus.chatSettings.inputOverFlowAutoFill.autoFillCommandInteraction.ignore"), // ignore command/slash and auto fill
        SKIP("chatPlus.chatSettings.inputOverFlowAutoFill.autoFillCommandInteraction.skip"), // auto fill next non command/slash
        CLEAR("chatPlus.chatSettings.inputOverFlowAutoFill.autoFillCommandInteraction.clear"), // clear auto fill

        ;

        val translatable: Component = Component.translatable(key)

        override fun getTranslatableName(): Component {
            return translatable
        }

    }

    @Serializable
    enum class QueueMode(key: String) : EnumTranslatableName {
        OVERWRITE("chatPlus.chatSettings.inputOverFlowAutoFill.queueMode.overwrite"),
        PREPEND("chatPlus.chatSettings.inputOverFlowAutoFill.queueMode.prepend"),
        APPEND("chatPlus.chatSettings.inputOverFlowAutoFill.queueMode.append"),

        ;

        val translatable: Component = Component.translatable(key)

        override fun getTranslatableName(): Component {
            return translatable
        }

    }
}