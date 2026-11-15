package com.ebicep.chatplus.translator.providers

import com.ebicep.chatplus.translator.*
import com.google.gson.Gson
import com.google.gson.JsonParser

object LibreTranslateProvider : TranslationProvider {

    override val backend = TranslationBackend.LIBRETRANSLATE
    override val rateLimit = TranslationRateLimit()

    override fun hasRequiredCredentials(credentials: TranslatorCredentials): Boolean {
        if (credentials.libreTranslateBaseUrl.isBlank()) {
            return false
        }
        val hostIsPublic = credentials.libreTranslateBaseUrl
            .trim()
            .lowercase()
            .let { url -> url.contains("libretranslate.com") }
        return !hostIsPublic || credentials.libreTranslateApiKey.isNotBlank()
    }

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
        val explicitSource = LanguageCodes.sourceCode(fromLang, backend)
            ?: return ProviderResult.Failure(FailureKind.UNSUPPORTED_LANGUAGE, fromLang.name)

        val payload = mutableMapOf(
            "q" to text,
            "source" to explicitSource,
            "target" to target,
            "format" to "text",
        )
        if (credentials.libreTranslateApiKey.isNotBlank()) {
            payload["api_key"] = credentials.libreTranslateApiKey
        }
        val base = credentials.libreTranslateBaseUrl.trimEnd('/')
        val response = TranslationHttp.post("$base/translate", Gson().toJson(payload), "application/json")
        if (response.responseCode == 0) {
            return ProviderResult.Failure(FailureKind.NETWORK, response.body)
        }
        return when (response.responseCode) {
            200 -> parseResponse(response.body)
            401, 403 -> ProviderResult.Failure(FailureKind.INVALID_KEY, response.body)
            429 -> {
                rateLimit.triggerCooldown()
                ProviderResult.Failure(FailureKind.RATE_LIMITED)
            }
            else -> ProviderResult.Failure(FailureKind.BAD_REQUEST, libreErrorDetail(response.responseCode, response.body))
        }
    }

    private fun libreErrorDetail(httpCode: Int, body: String): String {
        return try {
            val error = JsonParser.parseString(body).asJsonObject.get("error")?.asString
            error ?: "HTTP $httpCode: $body"
        } catch (_: Exception) {
            "HTTP $httpCode: $body"
        }
    }

    private fun parseResponse(body: String): ProviderResult {
        return try {
            val json = JsonParser.parseString(body).asJsonObject
            val translated = json.get("translatedText").asString
            val detectedElement = json.get("detectedLanguage")
            val detected = when {
                detectedElement == null || detectedElement.isJsonNull -> null
                detectedElement.isJsonObject -> detectedElement.asJsonObject.get("language")?.asString
                else -> detectedElement.asString
            }
            val sourceLang = detected?.let { LanguageManager.findLanguageFromProviderCode(backend, it) }
            ProviderResult.Success(translated, sourceLang)
        } catch (e: Exception) {
            ProviderResult.Failure(FailureKind.PARSE_ERROR, e.message)
        }
    }
}