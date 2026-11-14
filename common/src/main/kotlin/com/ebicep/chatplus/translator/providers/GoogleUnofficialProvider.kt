package com.ebicep.chatplus.translator.providers

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.translator.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object GoogleUnofficialProvider : TranslationProvider {

    private const val BASE_URL = "https://translate.googleapis.com/translate_a/single"

    override val backend = TranslationBackend.GOOGLE_UNOFFICIAL
    override val rateLimit = TranslationRateLimit()

    override fun hasRequiredCredentials(credentials: TranslatorCredentials): Boolean = true

    override fun translate(text: String, from: Language?, to: Language, credentials: TranslatorCredentials): ProviderResult {
        if (rateLimit.blocked) {
            return ProviderResult.Failure(FailureKind.RATE_LIMITED)
        }
        val fromLang = from ?: LanguageManager.autoLang
        val source = LanguageCodes.sourceCode(fromLang, backend)
        val target = LanguageCodes.targetCode(to, backend)
            ?: return ProviderResult.Failure(FailureKind.UNSUPPORTED_LANGUAGE, to.name)
        val encodedMessage = try {
            URLEncoder.encode(text, StandardCharsets.UTF_8.name())
        } catch (_: Exception) {
            return ProviderResult.Failure(FailureKind.BAD_REQUEST, "Failed to encode message")
        }
        val query = buildString {
            append(BASE_URL)
            append("?client=gtx&sl=").append(source).append("&tl=").append(target)
            append("&dt=t&q=").append(encodedMessage)
        }
        val response = TranslationHttp.get(query)
        if (response.responseCode == 0) {
            return ProviderResult.Failure(FailureKind.NETWORK, response.body)
        }
        if (response.responseCode == 429) {
            rateLimit.triggerCooldown()
            return ProviderResult.Failure(FailureKind.RATE_LIMITED)
        }
        if (response.responseCode != 200) {
            rateLimit.triggerCooldown()
            ChatPlus.LOGGER.error(response.body)
            return ProviderResult.Failure(FailureKind.BAD_REQUEST, "HTTP ${response.responseCode}")
        }
        return parseResponse(response.body, to)
    }

    private fun parseResponse(entity: String, target: Language): ProviderResult {
        return try {
            val gson = GsonBuilder().setLenient().create()
            val json: JsonArray = gson.fromJson(entity, JsonArray::class.java)
            val detectedSource = LanguageManager.findLanguageFromProviderCode(backend, json[2].asString)
            val translatedText = json[0].asJsonArray.joinToString(" ") { it.asJsonArray[0].asString }
            ProviderResult.Success(translatedText, detectedSource)
        } catch (e: Exception) {
            ChatPlus.LOGGER.error(e)
            ProviderResult.Failure(FailureKind.PARSE_ERROR, e.message)
        }
    }
}