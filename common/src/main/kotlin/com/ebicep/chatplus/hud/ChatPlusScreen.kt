package com.ebicep.chatplus.hud

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.Event
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ChatScreen
import org.apache.commons.lang3.StringUtils

object ChatPlusScreen {

    const val EDIT_BOX_HEIGHT = 14
    const val EDIT_BOX_HEIGHT_VANILLA = 12
    val EDIT_BOX_DISPLAY_HEIGHT: Int
        get() = if (Config.values.vanillaInputBox) EDIT_BOX_HEIGHT_VANILLA else EDIT_BOX_HEIGHT

    var lastMouseX = 0
    var lastMouseY = 0

    val messagesToSend: MutableList<String> = mutableListOf()
    var lastMessageSentTick = 0L

    fun splitChatMessage(message: String): List<String> {
        return if (message.length <= 256) {
            listOf(message)
        } else {
            val list = ArrayList<String>()
            var i = 0
            while (i < message.length) {
                var j = i + 256
                if (j >= message.length) {
                    j = message.length
                }
                list.add(message.substring(i, j))
                i = j
            }
            list
        }
    }

    fun normalizeChatMessage(message: String): String {
        return StringUtils.normalizeSpace(message.trim { it <= ' ' })
    }

}

data class ChatScreenKeyPressedEvent(
    val screen: ChatScreen,
    val keyCode: Int,
    val scanCode: Int,
    val modifiers: Int,
    var returnFunction: Boolean = false
) : Event

data class ChatScreenKeyReleasedEvent(
    val screen: ChatScreen,
    val keyCode: Int,
    val scanCode: Int,
    val modifiers: Int
) : Event

data class ChatScreenMouseClickedEvent(
    val screen: ChatScreen,
    val mouseX: Double,
    val mouseY: Double,
    val button: Int,
    var returnFunction: Boolean = false
) : Event

data class ChatScreenMouseScrolledEvent(
    val screen: ChatScreen,
    val mouseX: Double,
    val mouseY: Double,
    val amountX: Double,
    val amountY: Double,
    var returnFunction: Boolean = false
) : Event

data class ChatScreenMouseDraggedEvent(
    val screen: ChatScreen,
    val mouseX: Double,
    val mouseY: Double,
    val button: Int,
    val dragX: Double,
    val dragY: Double,
) : Event

data class ChatScreenMouseReleasedEvent(
    val screen: ChatScreen,
    val mouseX: Double,
    val mouseY: Double,
    val button: Int,
    var returnFunction: Boolean = false
) : Event

data class ChatScreenRenderEvent(
    val screen: ChatScreen,
    val guiGraphics: GuiGraphics,
    val mouseX: Int,
    val mouseY: Int,
    val partialTick: Float,
) : Event

data class ChatScreenInputBoxEditEvent(
    val screen: ChatScreen,
    val str: String,
    var returnFunction: Boolean = false
) : Event

data class ChatScreenInitPreEvent(
    val screen: ChatScreen,
) : Event

data class ChatScreenInitPostEvent(
    val screen: ChatScreen,
) : Event

data class ChatScreenCloseEvent(
    val screen: ChatScreen,
) : Event

data class ChatScreenSendMessagePreEvent(
    val screen: ChatScreen,
    var message: String,
    var returnFunction: Boolean = false
) : Event

data class ChatScreenSendMessagePostEvent(
    val screen: ChatScreen,
    var message: String,
    var sentMessage: String,
    val messageToSend: String,
    val normalizeChatMessage: String,
    val messages: List<String>,
    var dontSendMessage: Boolean = false
) : Event
