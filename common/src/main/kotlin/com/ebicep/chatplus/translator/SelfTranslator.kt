package com.ebicep.chatplus.translator

import com.ebicep.chatplus.features.InputOverFlowAutoFill
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatPlusScreen.splitChatMessage
import net.minecraft.client.Minecraft

class SelfTranslator(val toTranslate: String, val prefix: String) : Thread() {

    override fun run() {
        LanguageManager.languageSpeak?.let {
            val translator = Translator(toTranslate, LanguageManager.languageSelf, it)
            val translateResult = translator.translate(toTranslate) ?: return
            val messages = splitChatMessage(translateResult.translatedText)

            val connection = Minecraft.getInstance().player!!.connection
            val translatedMessage = messages[0]
            ChatManager.addSentMessage(translatedMessage)
            if (prefix.isEmpty()) {
                if (translatedMessage.startsWith("/")) {
                    connection.sendCommand(translatedMessage.substring(1))
                } else {
                    connection.sendChat(translatedMessage)
                }
            } else if (prefix.startsWith("/")) {
                connection.sendCommand("${prefix.substring(1).trim()}$translatedMessage")
            } else {
                connection.sendChat("$prefix $translatedMessage")
            }
            if (messages.size > 1) {
                InputOverFlowAutoFill.addToQueue(messages.subList(1, messages.size))
            }
        }
    }

}