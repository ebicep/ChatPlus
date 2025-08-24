package com.ebicep.chatplus.hud

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.serializers.KeyWithModifier
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.InputOverFlowAutoFill
import com.ebicep.chatplus.util.KeyUtil
import com.ebicep.chatplus.util.KeyUtil.isDown
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ChatScreen
import org.apache.commons.lang3.StringUtils
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

object ChatPlusScreen {

    const val EDIT_BOX_HEIGHT = 14
    const val EDIT_BOX_HEIGHT_VANILLA = 12
    val EDIT_BOX_DISPLAY_HEIGHT: Int
        get() = if (Config.values.vanillaInputBox) EDIT_BOX_HEIGHT_VANILLA else EDIT_BOX_HEIGHT

    var lastMouseX = 0
    var lastMouseY = 0

    var lastMessageSentTick = 0L

    val inputCooldowns: MutableMap<Any, AtomicBoolean> = IdentityHashMap()

    init {
        EventBus.register<ChatScreenCloseEvent>({ 100 }) {
            inputCooldowns.values.forEach { it.set(false) }
        }
    }

    fun sendChatMessage(chatScreen: ChatScreen? = null, message: String, addToSent: Boolean = true) {
        val normalizeChatMessage = normalizeChatMessage(message)
        if (normalizeChatMessage.isEmpty()) {
            return
        }
        sendChatMessage(chatScreen, splitChatMessage(normalizeChatMessage), addToSent)
    }

    private fun sendChatMessage(chatScreen: ChatScreen? = null, messages: List<String>, addToSent: Boolean = true) {
        if (messages.isEmpty()) {
            return
        }
        val screen = chatScreen ?: ChatScreen("")
        val messageToSend = screen.normalizeChatMessage(messages[0])
        if (addToSent) {
            ChatManager.addSentMessage(messageToSend)
        }
        if (messageToSend.startsWith("/")) {
            Minecraft.getInstance().player?.connection?.sendCommand(messageToSend.substring(1))
        } else {
            Minecraft.getInstance().player?.connection?.sendChat(messageToSend)
            if (messages.size > 1) {
                InputOverFlowAutoFill.addToQueue(messages.subList(1, messages.size))
            }
        }
    }

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

interface InputEvent : Event {
    val screen: ChatScreen
    var returnFunction: Boolean
}

class ChatScreenInputEvent(
    val inputEvent: InputEvent,
) {
    val screen: ChatScreen
        get() = inputEvent.screen

    var returnFunction: Boolean
        get() = inputEvent.returnFunction
        set(value) {
            inputEvent.returnFunction = value
        }

    fun isRelease(): Boolean {
        return inputEvent is ChatScreenKeyReleasedEvent || inputEvent is ChatScreenMouseReleasedEvent
    }

    fun isRelease(key: InputConstants.Key): Boolean {
        return isRelease(key.value)
    }

    fun isRelease(value: Int): Boolean {
        return if (KeyUtil.isMouseButton(value)) {
            inputEvent is ChatScreenMouseReleasedEvent && inputEvent.button == value
        } else {
            inputEvent is ChatScreenKeyReleasedEvent && inputEvent.keyCode == value
        }
    }

    fun checkRelease(keyWithModifier: KeyWithModifier, checkKeyDown: Boolean = true): Boolean {
        val inputCooldown = ChatPlusScreen.inputCooldowns.computeIfAbsent(keyWithModifier) { AtomicBoolean() }
        val value = keyWithModifier.key.value
        val isModifier = KeyUtil.isModifier(value)
        val hasModifier = keyWithModifier.modifier.toInt() != 0
        val keyReleased = isRelease(value) // (only check release)
        val modifierReleased = inputEvent is ChatScreenKeyReleasedEvent && KeyUtil.isModifier(inputEvent.keyCode)
        val isKey = !hasModifier && keyReleased // press x
        val isKeyWithModifier = hasModifier && (!modifierReleased || isModifier) && keyReleased // ctrl + X (only check X release UNLESS key is a modifier ctrl + shift)
        if (isKey || isKeyWithModifier) {
            inputCooldown.set(false)
            return true
        }
        if (inputCooldown.get()) {
            return true
        }
        if (checkKeyDown && keyWithModifier.isDown(this)) {
            inputCooldown.set(true)
            return false
        }
        return true

    }

    fun checkRelease(inputCooldownKey: Any, key: InputConstants.Key, checkKeyDown: Boolean = true): Boolean {
        val inputCooldown = ChatPlusScreen.inputCooldowns.computeIfAbsent(inputCooldownKey) { AtomicBoolean() }
        val value = key.value
        val keyReleased = isRelease(value)
        if (keyReleased) {
            inputCooldown.set(false)
            return true
        }
        if (inputCooldown.get()) {
            return true
        }
        if (checkKeyDown && key.isDown(this)) {
            inputCooldown.set(true)
            return false
        }
        return true
    }

    fun checkRelease(inputCooldownKey: Any, value: Int): Boolean {
        val inputCooldown = ChatPlusScreen.inputCooldowns.computeIfAbsent(inputCooldownKey) { AtomicBoolean() }
        if (isRelease(value)) {
            inputCooldown.set(false)
            return true
        }
        if (inputCooldown.get()) {
            return true
        }
        return false
    }

}

data class ChatScreenKeyPressedEvent(
    override val screen: ChatScreen,
    val keyCode: Int,
    val scanCode: Int,
    val modifiers: Int,
    override var returnFunction: Boolean = false,
) : InputEvent

data class ChatScreenKeyReleasedEvent(
    override val screen: ChatScreen,
    val keyCode: Int,
    val scanCode: Int,
    val modifiers: Int,
    override var returnFunction: Boolean = false, //unused
) : InputEvent

data class ChatScreenMouseClickedEvent(
    override val screen: ChatScreen,
    val mouseX: Double,
    val mouseY: Double,
    val button: Int,
    override var returnFunction: Boolean = false,
) : InputEvent

data class ChatScreenMouseScrolledEvent(
    val screen: ChatScreen,
    val mouseX: Double,
    val mouseY: Double,
    val amount: Double,
    var returnFunction: Boolean = false,
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
    override val screen: ChatScreen,
    val mouseX: Double,
    val mouseY: Double,
    val button: Int,
    override var returnFunction: Boolean = false,
) : InputEvent

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
    var returnFunction: Boolean = false,
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
    var returnFunction: Boolean = false,
) : Event

data class ChatScreenSendMessagePostEvent(
    val screen: ChatScreen,
    var message: String,
    var sentMessage: String,
    val messageToSend: String,
    val normalizeChatMessage: String,
    val messages: List<String>,
    var dontSendMessage: Boolean = false,
) : Event
