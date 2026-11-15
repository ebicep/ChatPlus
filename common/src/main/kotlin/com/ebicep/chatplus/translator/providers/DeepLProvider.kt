package com.ebicep.chatplus.translator.providers

import com.ebicep.chatplus.translator.*
import com.google.gson.JsonParser
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object DeepLProvider : TranslationProvider {

    override val backend = TranslationBackend.DEEPL
    override val rateLimit = TranslationRateLimit()

    override fun hasRequiredCredentials(credentials: TranslatorCredentials): Boolean =
        credentials.deeplApiKey.isNotBlank()

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
        val baseUrl = if (credentials.deeplApiKey.endsWith(":fx")) {
            "https://api-free.deepl.com/v2/translate"
        } else {
            "https://api.deepl.com/v2/translate"
        }
        val form = buildString {
            append("text=").append(URLEncoder.encode(text, StandardCharsets.UTF_8.name()))
            append("&target_lang=").append(target)
            if (source != null) {
                append("&source_lang=").append(source)
            }
        }
        val response = TranslationHttp.postForm(
            baseUrl,
            form,
            mapOf("Authorization" to "DeepL-Auth-Key ${credentials.deeplApiKey}"),
        )
        if (response.responseCode == 0) {
            return ProviderResult.Failure(FailureKind.NETWORK, response.body)
        }
        return when (response.responseCode) {
            200 -> parseResponse(response.body)
            403 -> ProviderResult.Failure(FailureKind.INVALID_KEY, response.body)
            429 -> {
                rateLimit.triggerCooldown()
                ProviderResult.Failure(FailureKind.RATE_LIMITED, response.body)
            }
            456 -> {
                rateLimit.triggerCooldown()
                ProviderResult.Failure(FailureKind.QUOTA, response.body)
            }
            else -> ProviderResult.Failure(FailureKind.BAD_REQUEST, "HTTP ${response.responseCode}: ${response.body}")
        }
    }

    private fun parseResponse(body: String): ProviderResult {
        return try {
            val translations = JsonParser.parseString(body).asJsonObject.getAsJsonArray("translations")
            val first = translations[0].asJsonObject
            val translated = first.get("text").asString
            val detected = first.get("detected_source_language")?.asString
            val sourceLang = detected?.let { LanguageManager.findLanguageFromProviderCode(backend, it) }
            ProviderResult.Success(translated, sourceLang)
        } catch (e: Exception) {
            ProviderResult.Failure(FailureKind.PARSE_ERROR, e.message)
        }
    }
}