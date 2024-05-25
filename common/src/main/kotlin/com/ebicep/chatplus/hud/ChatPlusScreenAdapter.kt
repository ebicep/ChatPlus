package com.ebicep.chatplus.hud

import com.ebicep.chatplus.IChatScreen
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatManager.sentMessages
import com.ebicep.chatplus.hud.ChatPlusScreen.messagesToSend
import com.ebicep.chatplus.hud.ChatPlusScreen.splitChatMessage
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.mixin.IMixinScreen
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.util.Mth

object ChatPlusScreenAdapter {

    fun handleInitPre(chatScreen: ChatScreen) {
        chatScreen as IChatScreen
        chatScreen.chatPlusWidth = chatScreen.width
        EventBus.post(ChatScreenInitPreEvent(chatScreen))
    }

    fun handleInitPost(chatScreen: ChatScreen) {
        EventBus.post(ChatScreenInitPostEvent(chatScreen))
    }

    fun handleRemoved(chatScreen: ChatScreen) {
        EventBus.post(ChatScreenCloseEvent(chatScreen))
        ChatManager.selectedTab.resetChatScroll()
        ChatManager.selectedTab.refreshDisplayedMessage()
    }

    fun handleOnEdited(chatScreen: ChatScreen, str: String): Boolean {
        return EventBus.post(ChatScreenInputBoxEditEvent(chatScreen, str)).returnFunction
    }

    fun handleKeyPressed(chatScreen: ChatScreen, pKeyCode: Int, pScanCode: Int, pModifiers: Int): Boolean {
        return EventBus.post(ChatScreenKeyPressedEvent(chatScreen, pKeyCode, pScanCode, pModifiers)).returnFunction
    }

    fun handlePageUpDown(up: Boolean) {
        if (up) {
            ChatManager.selectedTab.scrollChat(ChatManager.getLinesPerPage() - 1)
        } else {
            ChatManager.selectedTab.scrollChat(-ChatManager.getLinesPerPage() + 1)
        }
    }

    fun handleMouseScrolled(chatScreen: ChatScreen, mouseX: Double, mouseY: Double, amountX: Double, amountY: Double): Boolean {
        if (EventBus.post(ChatScreenMouseScrolledEvent(chatScreen, mouseX, mouseY, amountX, amountY)).returnFunction) {
            return true
        }
        // control = no scroll
        // shift = fine scroll
        // alt = triple scroll
        val window = Minecraft.getInstance().window.window
        if (InputConstants.isKeyDown(window, Config.values.keyNoScroll.value)) {
            return true
        }
        var delta = Mth.clamp(amountY, -1.0, 1.0)
        if (InputConstants.isKeyDown(window, Config.values.keyLargeScroll.value)) {
            delta *= 21.0
        } else if (!InputConstants.isKeyDown(window, Config.values.keyFineScroll.value)) {
            delta *= 7.0
        }
        ChatManager.selectedTab.scrollChat(delta.toInt())
        return false
    }

    fun handleMouseClicked(chatScreen: ChatScreen, mouseX: Double, mouseY: Double, pButton: Int): Boolean {
        return EventBus.post(ChatScreenMouseClickedEvent(chatScreen, mouseX, mouseY, pButton)).returnFunction
    }

    fun handleMouseReleased(chatScreen: ChatScreen, mouseX: Double, mouseY: Double, pButton: Int): Boolean {
        return EventBus.post(ChatScreenMouseReleasedEvent(chatScreen, mouseX, mouseY, pButton)).returnFunction
    }

    fun handleMouseDragged(chatScreen: ChatScreen, mouseX: Double, mouseY: Double, pButton: Int, pDragX: Double, pDragY: Double) {
        EventBus.post(ChatScreenMouseDraggedEvent(chatScreen, mouseX, mouseY, pButton, pDragX, pDragY))
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

    fun handleRenderHead(chatScreen: ChatScreen, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        ChatPlusScreen.lastMouseX = mouseX
        ChatPlusScreen.lastMouseY = mouseY
    }

    fun handleRenderTail(chatScreen: ChatScreen, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        EventBus.post(ChatScreenRenderEvent(chatScreen, guiGraphics, mouseX, mouseY, partialTick))
    }

    fun handleChatInput(chatScreen: ChatScreen, rawMessage: String): Boolean {
        chatScreen as IMixinChatScreen
        val sendMessageEvent = EventBus.post(ChatScreenSendMessagePreEvent(chatScreen, rawMessage))
        val minecraft = chatScreen.minecraft!!
        if (sendMessageEvent.returnFunction) {
            return minecraft.screen === chatScreen
        }
        val newMessage = sendMessageEvent.message
        val normalizeChatMessage = ChatPlusScreen.normalizeChatMessage(newMessage)
        if (normalizeChatMessage.isEmpty()) {
            return true
        }
        val messages = splitChatMessage(normalizeChatMessage)
        if (messages.isEmpty()) {
            return minecraft.screen === chatScreen
        }
        var sentMessage = messages[0]
        if (rawMessage != newMessage) {
            sentMessage = splitChatMessage(rawMessage)[0]
        }
        val messageToSend = messages[0]

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
            return minecraft.screen === chatScreen
        }

        ChatManager.addSentMessage(sentMessage)
        if (normalizeChatMessage.startsWith("/")) {
            minecraft.player!!.connection.sendCommand(messageToSend.substring(1))
        } else {
            minecraft.player!!.connection.sendChat(messageToSend)
            messagesToSend.addAll(messages.subList(1, messages.size))
        }
        return minecraft.screen === chatScreen
    }

}