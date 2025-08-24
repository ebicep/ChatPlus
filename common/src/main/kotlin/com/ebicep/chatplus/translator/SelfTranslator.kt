package com.ebicep.chatplus.translator

import com.ebicep.chatplus.hud.ChatPlusScreen

class SelfTranslator(val toTranslate: String, val prefix: String) : Thread() {

    override fun run() {
        LanguageManager.languageSpeak?.let {
            val translator = Translator(toTranslate, LanguageManager.languageSelf, it)
            val translateResult = translator.translate(toTranslate) ?: return
            val pre = if (prefix.isEmpty()) {
                ""
            } else if (prefix.startsWith("/")) {
                prefix.trim()
            } else {
                "$prefix "
            }
            val text = pre + translateResult.translatedText
            ChatPlusScreen.sendChatMessage(message = text)
        }
    }

}