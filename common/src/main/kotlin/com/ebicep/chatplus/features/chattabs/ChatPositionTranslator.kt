package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.config.MessageDirection
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatPositionTranslator.screenToChatX
import com.ebicep.chatplus.features.chattabs.ChatPositionTranslator.screenToChatY
import com.ebicep.chatplus.features.chattabs.ChatTab.ChatPlusGuiMessageLine
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatPlusScreen
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Style
import net.minecraft.util.Mth
import kotlin.math.min


data class ChatTabGetMessageAtEvent(
    val chatTab: ChatTab,
    val messageAtType: MessageAtType,
    val chatWindow: ChatWindow = chatTab.chatWindow,
    var mouseOperators: MutableList<OperatorXY> = mutableListOf(),
    var chatOperators: MutableList<OperatorXY> = mutableListOf(),
    var finalMouse: ValuesXY = ValuesXY(0.0, 0.0),
    var finalChat: ValuesXY = ValuesXY(0.0, 0.0),
) : Event {

    fun addMouseOperator(operator: OperatorXY) {
        mouseOperators.add(operator)
    }

    fun addChatOperator(operator: OperatorXY) {
        chatOperators.add(operator)
    }

    fun calculateFinalPositions(
        mX: Double,
        mY: Double
    ) {
        val originalMouse = ValuesXY(mX, mY)
        finalMouse = ValuesXY(mX, mY)
        mouseOperators.forEach { operator -> operator.apply(originalMouse, finalMouse) }

        val originalChat = ValuesXY(screenToChatX(chatTab, finalMouse.x), screenToChatY(chatTab, finalMouse.y))
        finalChat = ValuesXY(originalChat.x, originalChat.y)
        chatOperators.forEach { operator -> operator.apply(originalChat, finalChat) }
    }

}

class ValuesXY(var x: Double, var y: Double)

fun interface OperatorXY {
    fun apply(original: ValuesXY, current: ValuesXY)
}


enum class MessageAtType {

    HOVER,
    COMPONENT,
    INDEX,
    INTERNAL,

}

data class MessageAtResult(val messageAtEvent: ChatTabGetMessageAtEvent, val messageLine: ChatPlusGuiMessageLine?)


object ChatPositionTranslator {

    fun getHoveredOverMessageLine(chatTab: ChatTab): MessageAtResult {
        return getMessageLineAt(chatTab, MessageAtType.HOVER, ChatPlusScreen.lastMouseX.toDouble(), ChatPlusScreen.lastMouseY.toDouble())
    }

    fun getHoveredOverMessageLineInternal(chatTab: ChatTab): ChatPlusGuiMessageLine? {
        return getHoveredOverMessageLineInternal(
            chatTab,
            ChatPlusScreen.lastMouseX.toDouble(),
            ChatPlusScreen.lastMouseY.toDouble()
        )
    }

    fun getHoveredOverMessageLineInternal(chatTab: ChatTab, mX: Double, mY: Double): ChatPlusGuiMessageLine? {
        val messageAtEvent = EventBus.post(ChatTabGetMessageAtEvent(chatTab, MessageAtType.INTERNAL))
        messageAtEvent.calculateFinalPositions(mX, mY)
        return getMessageAtLineRelative(
            chatTab,
            messageAtEvent,
            messageAtEvent.finalChat.x,
            messageAtEvent.finalChat.y
        ).messageLine
    }

    fun getMessageLineAt(chatTab: ChatTab, messageAtType: MessageAtType, mX: Double, mY: Double): MessageAtResult {
        val messageAtEvent = EventBus.post(ChatTabGetMessageAtEvent(chatTab, messageAtType))
        messageAtEvent.calculateFinalPositions(mX, mY)
        return getMessageAtLineRelative(
            chatTab,
            messageAtEvent,
            messageAtEvent.finalChat.x,
            messageAtEvent.finalChat.y
        )
    }

    fun getMessageAtLineRelative(chatTab: ChatTab, messageAtEvent: ChatTabGetMessageAtEvent, relativeX: Double, relativeY: Double): MessageAtResult {
        val i = getMessageLineIndexAtRelative(chatTab, relativeX, relativeY)
        val size = chatTab.displayedMessages.size
        return if (i in 0 until size) {
            return MessageAtResult(messageAtEvent, chatTab.displayedMessages[size - i - 1])
        } else {
            MessageAtResult(messageAtEvent, null)
        }
    }

    fun getMessageAtLineRelative(chatTab: ChatTab, relativeX: Double, relativeY: Double): ChatPlusGuiMessageLine? {
        val i = getMessageLineIndexAtRelative(chatTab, relativeX, relativeY)
        val size = chatTab.displayedMessages.size
        return if (i in 0 until size) {
            return chatTab.displayedMessages[size - i - 1]
        } else {
            null
        }
    }

    fun screenToChatX(chatTab: ChatTab, pX: Double): Double {
        val chatWindow = chatTab.chatWindow
        return (pX - chatWindow.renderer.internalX) / chatWindow.renderer.scale
    }

    fun screenToChatY(chatTab: ChatTab, pY: Double): Double {
        val chatWindow = chatTab.chatWindow
        val yDiff: Double = chatWindow.renderer.internalY - pY
        return when (chatWindow.messageDirection) {
            MessageDirection.TOP_DOWN -> chatWindow.renderer.rescaledLinesPerPage - yDiff / (chatWindow.renderer.scale * chatWindow.renderer.lineHeight.toDouble())
            MessageDirection.BOTTOM_UP -> yDiff / (chatWindow.renderer.scale * chatWindow.renderer.lineHeight.toDouble())
        }
    }

    fun getMessageLineIndexAt(chatTab: ChatTab, mX: Double, mY: Double): Int {
        val messageAtEvent = EventBus.post(ChatTabGetMessageAtEvent(chatTab, MessageAtType.INDEX))
        messageAtEvent.calculateFinalPositions(mX, mY)
        return getMessageLineIndexAtRelative(chatTab, messageAtEvent.finalChat.x, messageAtEvent.finalChat.y)
    }

    private fun getMessageLineIndexAtRelative(chatTab: ChatTab, relativeX: Double, relativeY: Double): Int {
        val chatWindow = chatTab.chatWindow
        if (!ChatManager.isChatFocused() || Minecraft.getInstance().options.hideGui) {
            return -1
        }
        if (!(0.0 <= relativeX && relativeX <= Mth.floor(chatWindow.renderer.rescaledWidth.toDouble()))) {
            return -1
        }
        val i = min(chatWindow.renderer.rescaledLinesPerPage, chatTab.displayedMessages.size)
        if (!(0.0 <= relativeY && relativeY < i.toDouble())) {
            return -1
        }
        val j = Mth.floor(relativeY + chatTab.chatScrollbarPos.toDouble())
        if (j < 0 || j >= chatTab.displayedMessages.size) {
            return -1
        }
        return j
    }

    fun getComponentStyleAt(chatTab: ChatTab, mX: Double, mY: Double): Style? {
        val messageLineAt = getMessageLineAt(chatTab, MessageAtType.COMPONENT, mX, mY)
        return if (messageLineAt.messageLine != null) {
            Minecraft.getInstance().font.splitter.componentStyleAtWidth(messageLineAt.messageLine.line.content(), Mth.floor(messageLineAt.messageAtEvent.finalChat.x))
        } else {
            null
        }
    }

}