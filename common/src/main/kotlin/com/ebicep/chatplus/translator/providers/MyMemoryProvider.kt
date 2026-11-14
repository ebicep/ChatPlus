package com.ebicep.chatplus.translator.providers

import com.ebicep.chatplus.translator.*
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object MyMemoryProvider : TranslationProvider {

    override val backend = TranslationBackend.MYMEMORY
    override val rateLimit = TranslationRateLimit()

    override fun hasRequiredCredentials(credentials: TranslatorCredentials): Boolean = true

    override fun translate(text: String, from: Language?, to: Language, credentials: TranslatorCredentials): ProviderResult {
        if (rateLimit.blocked) {
            return ProviderResult.Failure(FailureKind.RATE_LIMITED)
        }
        val target = LanguageCodes.targetCode(to, backend)
            ?: return ProviderResult.Failure(FailureKind.UNSUPPORTED_LANGUAGE, to.name)
        val fromLang = from ?: LanguageManager.autoLang
        val source = LanguageCodes.sourceCode(fromLang, backend)
            ?: return ProviderResult.Failure(FailureKind.UNSUPPORTED_LANGUAGE, fromLang.name)
        val encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.name())
        val langPair = URLEncoder.encode("$source|$target", StandardCharsets.UTF_8.name())
        val url = buildString {
            append("https://api.mymemory.translated.net/get?q=").append(encodedText)
            append("&langpair=").append(langPair)
            if (credentials.myMemoryEmail.isNotBlank()) {
                append("&de=").append(URLEncoder.encode(credentials.myMemoryEmail, StandardCharsets.UTF_8.name()))
            }
        }
        val response = TranslationHttp.get(url)
        if (response.responseCode == 0) {
            return ProviderResult.Failure(FailureKind.NETWORK, response.body)
        }
        if (response.responseCode == 429) {
            rateLimit.triggerCooldown()
            return ProviderResult.Failure(FailureKind.RATE_LIMITED)
        }
        if (response.responseCode != 200) {
            return ProviderResult.Failure(FailureKind.BAD_REQUEST, "HTTP ${response.responseCode}: ${response.body}")
        }
        return parseResponse(response.body)
    }

    private fun parseResponse(body: String): ProviderResult {
        return try {
            val root = JsonParser.parseString(body).asJsonObject
            val responseData = root.getAsJsonObject("responseData")
            val translated = responseData.get("translatedText").asString
            val quotaFinished = root.get("quotaFinished")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
            val statusOk = responseStatusOk(root)
            if (!statusOk || quotaFinished) {
                val quotaMessage = translated.contains("YOU USED ALL AVAILABLE", ignoreCase = true)
                if (quotaFinished || quotaMessage) {
                    rateLimit.triggerCooldown()
                    return ProviderResult.Failure(FailureKind.QUOTA, translated)
                }
                return ProviderResult.Failure(FailureKind.BAD_REQUEST, translated)
            }
            val detectedFromResponse = responseData.get("detectedLanguage")?.takeIf { !it.isJsonNull }?.asString
            val matches = root.get("matches")?.takeIf { it.isJsonArray }?.asJsonArray
            val matchSource = if (matches != null && matches.size() > 0) {
                matches[0].asJsonObject.get("source")?.asString
            } else {
                null
            }
            val detected = detectedFromResponse ?: matchSource
            val sourceLang = detected?.let { LanguageManager.findLanguageFromProviderCode(backend, it) }
            ProviderResult.Success(translated, sourceLang)
        } catch (e: Exception) {
            ProviderResult.Failure(FailureKind.PARSE_ERROR, e.message)
        }
    }

    private fun responseStatusOk(root: JsonObject): Boolean {
        if (!root.has("responseStatus")) {
            return true
        }
        val el = root.get("responseStatus")
        if (el.isJsonNull) {
            return true
        }
        return when {
            el.isJsonPrimitive && el.asJsonPrimitive.isNumber -> el.asInt == 200
            el.isJsonPrimitive -> el.asString == "200"
            else -> false
        }
    }
}