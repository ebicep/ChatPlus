package com.ebicep.chatplus.translator.providers

import com.ebicep.chatplus.translator.*
import com.google.gson.JsonParser
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.random.Random

object BaiduProvider : TranslationProvider {

    override val backend = TranslationBackend.BAIDU
    override val rateLimit = TranslationRateLimit()

    override fun hasRequiredCredentials(credentials: TranslatorCredentials): Boolean =
        credentials.baiduAppId.isNotBlank() && credentials.baiduSecretKey.isNotBlank()

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
            ?: return ProviderResult.Failure(FailureKind.UNSUPPORTED_LANGUAGE, fromLang.name)
        val salt = Random.nextInt(10000, 99999).toString()
        val signInput = credentials.baiduAppId + text + salt + credentials.baiduSecretKey
        val sign = md5(signInput)
        val query = buildString {
            append("https://fanyi-api.baidu.com/api/trans/vip/translate?")
            append("q=").append(URLEncoder.encode(text, StandardCharsets.UTF_8.name()))
            append("&from=").append(source)
            append("&to=").append(target)
            append("&appid=").append(credentials.baiduAppId)
            append("&salt=").append(salt)
            append("&sign=").append(sign)
        }
        val response = TranslationHttp.get(query)
        if (response.responseCode == 0) {
            return ProviderResult.Failure(FailureKind.NETWORK, response.body)
        }
        if (response.responseCode == 429) {
            rateLimit.triggerCooldown()
            return ProviderResult.Failure(FailureKind.RATE_LIMITED)
        }
        return parseResponse(response)
    }

    private fun parseResponse(response: TranslationHttp.HttpResponse): ProviderResult {
        return try {
            val root = JsonParser.parseString(response.body).asJsonObject
            if (root.has("error_code")) {
                val code = root.get("error_code").asString
                if (code != "52000") {
                    return when (code) {
                        "52003", "54001", "58000", "90107" ->
                            ProviderResult.Failure(FailureKind.INVALID_KEY, root.get("error_msg")?.asString)
                        "58001" ->
                            ProviderResult.Failure(FailureKind.UNSUPPORTED_LANGUAGE, root.get("error_msg")?.asString)
                        "58002" ->
                            ProviderResult.Failure(FailureKind.PROVIDER_DISABLED, root.get("error_msg")?.asString)
                        "54004", "54005" -> {
                            rateLimit.triggerCooldown()
                            ProviderResult.Failure(FailureKind.QUOTA, root.get("error_msg")?.asString)
                        }
                        else -> ProviderResult.Failure(FailureKind.BAD_REQUEST, root.get("error_msg")?.asString)
                    }
                }
            }
            if (response.responseCode != 200) {
                return ProviderResult.Failure(FailureKind.BAD_REQUEST, "HTTP ${response.responseCode}")
            }
            val translations = root.getAsJsonArray("trans_result")
            val translated = translations.joinToString(" ") { it.asJsonObject.get("dst").asString }
            val detected = root.get("from")?.asString
            val sourceLang = detected?.let { LanguageManager.findLanguageFromProviderCode(backend, it) }
            ProviderResult.Success(translated, sourceLang)
        } catch (e: Exception) {
            ProviderResult.Failure(FailureKind.PARSE_ERROR, e.message)
        }
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}