package com.ebicep.chatplus.translator

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.util.ComponentUtil
import kotlinx.serialization.Serializable
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import java.util.regex.Pattern

open class Translator(val message: String, val from: Language?, val to: Language, val filtered: Boolean = true) : Thread() {

    override fun run() {
        ChatPlus.LOGGER.debug("Translating message: {} | {} -> {} | filtered: {}", message, from, to, filtered)

        val (matchedRegex, textToTranslate) = if (filtered) filterText(message) else null to message
        if (filtered && matchedRegex == null) {
            ChatPlus.LOGGER.debug("No regex match found for filtered message")
            return
        }
        val translatedMessage = translate(textToTranslate) ?: run {
            ChatPlus.LOGGER.debug("Translation failed for: $textToTranslate")
            return
        }
        if (translatedMessage.translatedText.trim().equals(textToTranslate, ignoreCase = true)) {
            ChatPlus.LOGGER.debug("$message is the same after translation")
            onTranslateSameMessage()
            return
        }
        ChatPlus.LOGGER.debug("Translated message: ${translatedMessage.translatedText}")
        onTranslate(matchedRegex, translatedMessage, translatedMessage.from?.name)
    }

    private fun filterText(text: String): Pair<String?, String> {
        for (regexMatch in Config.values.translatorRegexes) {
            val pattern = regexMatch.pattern.takeIf { it.isNotEmpty() } ?: continue
            val matcher = Pattern.compile(pattern).matcher(text)
            if (matcher.find()) {
                val matchedRegex = matcher.group(0)
                val filteredText = text.replace(matchedRegex, "").trim()
                return matchedRegex to filteredText
            }
        }
        return null to text
    }

    open fun onTranslateSameMessage() {

    }

    open fun onTranslate(matchedRegex: String?, translatedMessage: TranslateResult, fromLanguage: String?) {
        Minecraft.getInstance().player?.sendSystemMessage(
            ComponentUtil.literal(
                (matchedRegex ?: "") + translatedMessage.translatedText + " (" + (fromLanguage ?: "Unknown") + ")",
                ChatFormatting.GREEN
            )
        )
    }

    fun translate(text: String): TranslateResult? {
        if (from == to) {
            return null
        }
        if (text.trim().isEmpty()) {
            return null
        }
        if (GoogleRequester.accessDenied) {
            return null
        }
        //Use free ones later
        val google = GoogleRequester()
        val transRequest: RequestResult = if (from == null) google.translateAuto(text, to) else google.performTranslationRequest(text, from, to)
        if (transRequest.code != 200) {
            logException(transRequest)
            return null
        }
        if (transRequest.from == null) {
            return null
        }
        return TranslateResult(transRequest.message.trim(), transRequest.from)
    }

    private fun logException(transRequest: RequestResult) {
        when (transRequest.code) {
            1 -> ChatPlus.LOGGER.error("Cannot connect to translation server. Is player offline?")
            2 -> ChatPlus.LOGGER.error(transRequest.message)
            411 -> ChatPlus.LOGGER.error("Google API >> API call error")
            429 -> ChatPlus.LOGGER.warn("Google denied access to translation API. Pausing translation for 5 minutes")
            403 -> ChatPlus.LOGGER.error("Google API >> Exceeded API quota / User rate limit reached")
            400 -> ChatPlus.LOGGER.error("Google API >> API key invalid")
            500 -> ChatPlus.LOGGER.error("Google API >> Failed to determine source language: " + transRequest.message)
            else -> ChatPlus.LOGGER.error("Unknown error/Server side failure: " + transRequest.message)
        }
    }

}


data class TranslateResult(val translatedText: String, val from: Language?)

@Serializable
data class RegexMatch(var match: String, var senderNameGroupIndex: Int)