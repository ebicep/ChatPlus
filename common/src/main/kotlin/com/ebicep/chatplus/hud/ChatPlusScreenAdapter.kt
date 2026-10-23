package com.ebicep.chatplus.hud

import com.ebicep.chatplus.IChatScreen
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.Config.values
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.InputOverFlowAutoFill
import com.ebicep.chatplus.hud.ChatManager.sentMessages
import com.ebicep.chatplus.hud.ChatPlusScreen.splitChatMessage
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.mixin.IMixinScreen
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.util.Mth

object ChatPlusScreenAdapter {

    fun handleInitPre(chatScreen: ChatScreen) {
        chatScreen as IChatScreen
        val w = values.inputBoxSettings.getCalculatedWidth()
        chatScreen.chatPlusWidth = if (w < 0 || values.vanillaInputBox) chatScreen.width else w
        EventBus.post(ChatScreenInitPreEvent(chatScreen))
    }

    fun handleInitPost(chatScreen: ChatScreen) {
        EventBus.post(ChatScreenInitPostEvent(chatScreen))
    }

    fun handleRemoved(chatScreen: ChatScreen) {
        EventBus.post(ChatScreenCloseEvent(chatScreen))
        ChatManager.globalSelectedTab.resetChatScroll()
//        ChatManager.selectedTab.refreshDisplayedMessage()
    }

    fun handleOnEdited(chatScreen: ChatScreen, str: String): Boolean {
        return EventBus.post(ChatScreenInputBoxEditEvent(chatScreen, str)).returnFunction
    }

    fun handleKeyPressed(chatScreen: ChatScreen, keyEvent: KeyEvent): Boolean {
        val event = ChatScreenKeyPressedEvent(chatScreen, keyEvent)
        EventBus.post(ChatScreenInputEvent(event))
        return event.returnFunction
    }

    fun handleKeyReleased(chatScreen: ChatScreen, keyEvent: KeyEvent) {
        val event = ChatScreenKeyReleasedEvent(chatScreen, keyEvent)
        EventBus.post(ChatScreenInputEvent(event))
    }

    fun handlePageUpDown(up: Boolean) {
        val globalSelectedTab = ChatManager.globalSelectedTab
        if (up) {
            globalSelectedTab.scrollChat(globalSelectedTab.chatWindow.renderer.getLinesPerPage() - 1)
        } else {
            globalSelectedTab.scrollChat(-globalSelectedTab.chatWindow.renderer.getLinesPerPage() + 1)
        }
    }

    fun handleMouseScrolled(chatScreen: ChatScreen, mouseX: Double, mouseY: Double, amountX: Double, amountY: Double): Boolean {
        if (EventBus.post(ChatScreenMouseScrolledEvent(chatScreen, mouseX, mouseY, amountX, amountY)).returnFunction) {
            return true
        }
        return scrollChat(amountY)
    }

    fun scrollChat(amountY: Double): Boolean {
        // control = no scroll
        // shift = fine scroll
        // alt = triple scroll
        if (InputConstants.isKeyDown(values.keyNoScroll.value)) {
            return true
        }
        val scrollAmount = if (values.invertedScrolling) -amountY else amountY
        var delta = Mth.clamp(scrollAmount, -1.0, 1.0)
        if (InputConstants.isKeyDown(values.keyLargeScroll.value)) {
            delta *= 21.0
        } else if (!InputConstants.isKeyDown(values.keyFineScroll.value)) {
            delta *= 7.0
        }
        ChatManager.globalSelectedTab.scrollChat(delta.toInt())
        return false
    }

    fun handleMouseClicked(chatScreen: ChatScreen, mouseButtonEvent: MouseButtonEvent): Boolean {
        val event = ChatScreenMouseClickedEvent(chatScreen, mouseButtonEvent)
        EventBus.post(ChatScreenInputEvent(event))
        return event.returnFunction
    }

    fun handleMouseReleased(chatScreen: ChatScreen, mouseButtonEvent: MouseButtonEvent): Boolean {
        val event = ChatScreenMouseReleasedEvent(chatScreen, mouseButtonEvent)
        EventBus.post(ChatScreenInputEvent(event))
        return event.returnFunction
    }

    fun handleMouseDragged(chatScreen: ChatScreen, mouseButtonEvent: MouseButtonEvent, deltaX: Double, deltaY: Double) {
        EventBus.post(ChatScreenMouseDraggedEvent(chatScreen, mouseButtonEvent, deltaX, deltaY))
    }

    fun handleMoveInHistory(chatScreen: ChatScreen, pMsgPos: Int) {
        chatScreen as IMixinChatScreen
        var i = chatScreen.historyPos + pMsgPos
        val j = sentMessages.size
        i = Mth.clamp(i, 0, j)
        if (i != chatScreen.historyPos) {
            val input = chatScreen.input
            if (i == j) {
                chatScreen.historyPos = j
                input.value = chatScreen.historyBuffer
            } else {
                if (chatScreen.historyPos == j) {
                    chatScreen.historyBuffer = input.value
                }
                input.value = sentMessages[i]
                chatScreen.commandSuggestions.setAllowSuggestions(false)
                chatScreen.historyPos = i
            }
            (chatScreen as IMixinScreen).callSetInitialFocus(input)
        }
    }

    fun handleRenderHead(chatScreen: ChatScreen, guiGraphicsExtractor: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        ChatPlusScreen.lastMouseX = mouseX
        ChatPlusScreen.lastMouseY = mouseY
    }

    fun handleRenderTail(chatScreen: ChatScreen, guiGraphicsExtractor: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        EventBus.post(ChatScreenRenderEvent(chatScreen, guiGraphicsExtractor, mouseX, mouseY, partialTick))
    }

    fun handleChatInput(chatScreen: ChatScreen, rawMessage: String): Boolean {
        chatScreen as IMixinChatScreen
        val sendMessageEvent = EventBus.post(ChatScreenSendMessagePreEvent(chatScreen, rawMessage))
        val minecraft = chatScreen.minecraft!!
        if (sendMessageEvent.returnFunction) {
            return minecraft.gui.screen() === chatScreen
        }
        val newMessage = sendMessageEvent.message
        val normalizeChatMessage = ChatPlusScreen.normalizeChatMessage(newMessage)
        if (normalizeChatMessage.isEmpty()) {
            return true
        }
        val messages = splitChatMessage(normalizeChatMessage)
        if (messages.isEmpty()) {
            return minecraft.gui.screen() === chatScreen
        }
        var sentMessage = messages[0]
        if (rawMessage != newMessage) {
            sentMessage = splitChatMessage(rawMessage)[0]
        }
        val messageToSend = chatScreen.normalizeChatMessage(messages[0])

        if (EventBus.post(
                ChatScreenSendMessagePostEvent(
                    chatScreen,
                    newMessage,
                    sentMessage,
                    messageToSend,
                    normalizeChatMessage,
                    messages
                )
            ).dontSendMessage
        ) {
            return minecraft.gui.screen() === chatScreen
        }

        ChatManager.addSentMessage(sentMessage)
        if (normalizeChatMessage.startsWith("/")) {
            minecraft.player!!.connection.sendCommand(messageToSend.substring(1))
        } else {
            minecraft.player!!.connection.sendChat(messageToSend)
            if (messages.size > 1) {
                InputOverFlowAutoFill.addToQueue(messages.subList(1, messages.size))
            }
        }
        return minecraft.gui.screen() === chatScreen
    }

}