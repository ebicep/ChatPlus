package com.ebicep.chatplus.translator.providers

import com.ebicep.chatplus.translator.*
import com.google.gson.Gson
import com.google.gson.JsonParser

object AzureTranslatorProvider : TranslationProvider {

    override val backend = TranslationBackend.AZURE
    override val rateLimit = TranslationRateLimit()

    override fun hasRequiredCredentials(credentials: TranslatorCredentials): Boolean =
        credentials.azureTranslatorKey.isNotBlank()

    override fun translate(text: String, from: Language?, to: Language, credentials: TranslatorCredentials): ProviderResult {
        if (rateLimit.blocked) {
            return ProviderResult.Failure(FailureKind.RATE_LIMITED)
        }
        if (!hasRequiredCredentials(credentials)) {
            return ProviderResult.Failure(FailureKind.MISSING_CREDENTIALS)
        }
        val target = LanguageCodes.targetCode(to, backend)
            ?: return ProviderResult.Failure(FailureKind.UNSUPPORTED_LANGUAGE, to.name)
        val fromLang = from ?: LanguageManager.autoLang
        val source = LanguageCodes.sourceCode(fromLang, backend)
        if (source == null && !LanguageCodes.isAutoDetect(fromLang)) {
            return ProviderResult.Failure(FailureKind.UNSUPPORTED_LANGUAGE, fromLang.name)
        }
        val query = buildString {
            append("https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&to=")
            append(target)
            if (source != null) {
                append("&from=").append(source)
            }
        }
        val body = Gson().toJson(listOf(mapOf("Text" to text)))
        val headers = buildMap {
            put("Ocp-Apim-Subscription-Key", credentials.azureTranslatorKey)
            if (credentials.azureTranslatorRegion.isNotBlank()) {
                put("Ocp-Apim-Subscription-Region", credentials.azureTranslatorRegion)
            }
        }
        val response = TranslationHttp.post(
            query,
            body,
            "application/json",
            headers,
        )
        if (response.responseCode == 0) {
            return ProviderResult.Failure(FailureKind.NETWORK, response.body)
        }
        return when (response.responseCode) {
            200 -> parseResponse(response.body)
            401 -> ProviderResult.Failure(FailureKind.INVALID_KEY, response.body)
            403 -> {
                rateLimit.triggerCooldown()
                ProviderResult.Failure(FailureKind.QUOTA, response.body)
            }
            429 -> {
                rateLimit.triggerCooldown()
                ProviderResult.Failure(FailureKind.RATE_LIMITED)
            }
            else -> ProviderResult.Failure(FailureKind.BAD_REQUEST, "HTTP ${response.responseCode}: ${response.body}")
        }
    }

    private fun parseResponse(body: String): ProviderResult {
        return try {
            val first = JsonParser.parseString(body).asJsonArray[0].asJsonObject
            val translations = first.getAsJsonArray("translations")[0].asJsonObject
            val translated = translations.get("text").asString
            val detected = first.getAsJsonObject("detectedLanguage")?.get("language")?.asString
            val sourceLang = detected?.let { LanguageManager.findLanguageFromProviderCode(backend, it) }
            ProviderResult.Success(translated, sourceLang)
        } catch (e: Exception) {
            ProviderResult.Failure(FailureKind.PARSE_ERROR, e.message)
        }
    }
}