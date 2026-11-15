package com.ebicep.chatplus.translator

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.util.ClientExecutor
import com.ebicep.chatplus.util.ComponentUtil
import kotlinx.serialization.Serializable
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import java.util.regex.Pattern

open class Translator(
    val message: String,
    val from: Language?,
    val to: Language,
    val filtered: Boolean = true,
) {

    fun start() {
        TranslationDispatcher.submit { run() }
    }

    fun run() {
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
            Minecraft.getInstance().execute { onTranslateSameMessage() }
            return
        }
        ChatPlus.LOGGER.debug("Translated message: ${translatedMessage.translatedText}")
        val fromLanguage = translatedMessage.from?.name
        Minecraft.getInstance().execute {
            onTranslate(matchedRegex, translatedMessage, fromLanguage)
        }
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
        ClientExecutor.runOnClient {
            Minecraft.getInstance().player?.sendSystemMessage(
                ComponentUtil.literalIgnored(
                    (matchedRegex ?: "") + translatedMessage.translatedText + " (" + (fromLanguage ?: "Unknown") + ")",
                    ComponentUtil.LiteralIgnoredType.TRANSLATE,
                ).withStyle(ChatFormatting.GREEN),
            )
        }
    }

    fun translate(text: String): TranslateResult? {
        if (from == to) {
            return null
        }
        if (text.trim().isEmpty()) {
            return null
        }
        val backend = Config.values.translatorBackend
        val credentials = Config.values.translatorCredentials
        val provider = TranslationProviders.get(backend)
        if (provider.rateLimit.blocked) {
            return null
        }
        if (!provider.hasRequiredCredentials(credentials)) {
            TranslationErrors.log(backend, ProviderResult.Failure(FailureKind.MISSING_CREDENTIALS))
            return null
        }
        when (val result = provider.translate(text, from, to, credentials)) {
            is ProviderResult.Success -> {
                val sourceLang = when {
                    from != null && !LanguageCodes.isAutoDetect(from) -> from
                    else -> result.detectedSource
                }
                return TranslateResult(result.text.trim(), sourceLang)
            }
            is ProviderResult.Failure -> {
                TranslationErrors.log(backend, result)
                return null
            }
        }
    }

}

data class TranslateResult(val translatedText: String, val from: Language?)

@Serializable
data class RegexMatch(var match: String, var senderNameGroupIndex: Int)