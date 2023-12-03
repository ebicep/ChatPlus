package com.ebicep.chatplus.translator

import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatPlusScreen.Companion.splitChatMessage
import net.minecraft.client.Minecraft

class SelfTranslator(val message: String, val prefix: String) : Thread() {

    override fun run() {
        languageSpeak?.let {
            val translator = Translator(message, languageSelf, it)
            val translateResult = translator.translate(message) ?: return
            splitChatMessage(translateResult.translatedText).forEach { splitMessage ->
                ChatManager.addSentMessage(splitMessage)
                if (prefix.isEmpty()) {
                    Minecraft.getInstance().player!!.connection.sendChat(splitMessage)
                } else {
                    Minecraft.getInstance().player!!.connection.sendChat("$prefix $splitMessage")
                }
            }
        }
    }

}