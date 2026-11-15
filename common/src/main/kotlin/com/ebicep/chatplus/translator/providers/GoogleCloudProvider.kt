package com.ebicep.chatplus.translator.providers

import com.ebicep.chatplus.translator.*
import com.google.gson.JsonParser
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object GoogleCloudProvider : TranslationProvider {

    override val backend = TranslationBackend.GOOGLE_CLOUD
    override val rateLimit = TranslationRateLimit()

    override fun hasRequiredCredentials(credentials: TranslatorCredentials): Boolean =
        credentials.googleCloudApiKey.isNotBlank()

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
        val encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.name())
        val url = "https://translation.googleapis.com/language/translate/v2?key=${credentials.googleCloudApiKey}"
        val form = buildString {
            append("q=").append(encodedText).append("&target=").append(target).append("&format=text")
            if (source != null) {
                append("&source=").append(source)
            }
        }
        val response = TranslationHttp.postForm(url, form)
        if (response.responseCode == 0) {
            return ProviderResult.Failure(FailureKind.NETWORK, response.body)
        }
        return when (response.responseCode) {
            200 -> parseResponse(response.body)
            400 -> if (response.body.contains("API_KEY_INVALID", ignoreCase = true)
                || response.body.contains("API key not valid", ignoreCase = true)
            ) {
                ProviderResult.Failure(FailureKind.INVALID_KEY, response.body)
            } else {
                ProviderResult.Failure(FailureKind.BAD_REQUEST, response.body)
            }
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
            val root = JsonParser.parseString(body).asJsonObject
            val translation = root.getAsJsonObject("data").getAsJsonArray("translations")[0].asJsonObject
            val translated = translation.get("translatedText").asString
            val detected = translation.get("detectedSourceLanguage")?.asString
            val sourceLang = detected?.let { LanguageManager.findLanguageFromProviderCode(backend, it) }
            ProviderResult.Success(translated, sourceLang)
        } catch (e: Exception) {
            ProviderResult.Failure(FailureKind.PARSE_ERROR, e.message)
        }
    }
}