package com.ebicep.chatplus.translator

import com.ebicep.chatplus.hud.ChatPlusScreen
import com.ebicep.chatplus.util.ClientExecutor

class SelfTranslator(val toTranslate: String, val prefix: String) {

    fun start() {
        TranslationDispatcher.submit { run() }
    }

    fun run() {
        LanguageManager.languageSpeak?.let {
            val translator = Translator(toTranslate, LanguageManager.languageSelf, it)
            val translateResult = translator.translate(toTranslate) ?: return
            val pre = if (prefix.isEmpty()) {
                ""
            } else if (prefix.startsWith("/")) {
                prefix
            } else {
                "$prefix "
            }
            val text = pre + translateResult.translatedText
            ClientExecutor.runOnClient {
                ChatPlusScreen.sendChatMessage(message = text)
            }
        }
    }

}