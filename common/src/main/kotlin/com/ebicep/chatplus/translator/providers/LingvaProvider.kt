package com.ebicep.chatplus.translator.providers

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.translator.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import java.nio.charset.StandardCharsets

object LingvaProvider : TranslationProvider {

    private val graphqlQuery = """
        query(${'$'}source: String, ${'$'}target: String, ${'$'}query: String!) {
            translation(source: ${'$'}source, target: ${'$'}target, query: ${'$'}query) {
                target { text }
                source { detected { code } }
            }
        }
    """.trimIndent()

    override val backend = TranslationBackend.LINGVA
    override val rateLimit = TranslationRateLimit()

    private val gson = GsonBuilder().setLenient().create()

    private val requestHeaders = mapOf(
        "Accept" to "application/json",
        "User-Agent" to "ChatPlus",
    )

    override fun hasRequiredCredentials(credentials: TranslatorCredentials): Boolean =
        credentials.lingvaBaseUrl.isNotBlank()

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
        val base = normalizeBaseUrl(credentials.lingvaBaseUrl)
        val encodedQuery = encodePathSegment(text)
        val url = "$base/api/v1/$source/$target/$encodedQuery"
        val response = TranslationHttp.get(url, requestHeaders)
        if (response.responseCode == 0) {
            return ProviderResult.Failure(FailureKind.NETWORK, response.body)
        }
        if (response.responseCode == 429) {
            rateLimit.triggerCooldown()
            return ProviderResult.Failure(FailureKind.RATE_LIMITED)
        }
        val restResult = parseApiJson(response.body, response.responseCode)
        if (restResult is ProviderResult.Success) {
            return restResult
        }
        if (restResult is ProviderResult.Failure && restResult.kind != FailureKind.PARSE_ERROR
            && restResult.kind != FailureKind.BAD_REQUEST
        ) {
            return restResult
        }
        val graphqlResult = translateGraphQl(base, source, target, text)
        if (graphqlResult is ProviderResult.Success) {
            return graphqlResult
        }
        return preferMoreSpecificFailure(restResult, graphqlResult)
    }

    private fun preferMoreSpecificFailure(
        rest: ProviderResult,
        graphQl: ProviderResult,
    ): ProviderResult {
        val unavailable = "Lingva could not translate (instance down or Google scraper blocked). Try another mirror URL or a different translation service."
        if (rest is ProviderResult.Failure && rest.kind == FailureKind.PARSE_ERROR
            && rest.detail?.contains("Missing translation", ignoreCase = true) == true
        ) {
            if (graphQl is ProviderResult.Failure && graphQl.detail != null) {
                return ProviderResult.Failure(FailureKind.BAD_REQUEST, "$unavailable GraphQL: ${graphQl.detail}")
            }
            return ProviderResult.Failure(FailureKind.BAD_REQUEST, unavailable)
        }
        return rest
    }

    private fun translateGraphQl(
        base: String,
        source: String,
        target: String,
        text: String,
    ): ProviderResult {
        val payload = gson.toJson(
            mapOf(
                "query" to graphqlQuery,
                "variables" to mapOf(
                    "source" to source,
                    "target" to target,
                    "query" to text,
                ),
            ),
        )
        val response = TranslationHttp.post(
            "$base/api/graphql",
            payload,
            "application/json",
            requestHeaders,
        )
        if (response.responseCode == 0) {
            return ProviderResult.Failure(FailureKind.NETWORK, response.body)
        }
        if (response.responseCode != 200) {
            return ProviderResult.Failure(FailureKind.BAD_REQUEST, "GraphQL HTTP ${response.responseCode}")
        }
        return parseGraphQlJson(response.body)
    }

    private fun parseApiJson(body: String, httpCode: Int): ProviderResult {
        val jsonBody = prepareJsonBody(body)
            ?: return ProviderResult.Failure(
                FailureKind.PARSE_ERROR,
                "Instance did not return JSON (wrong URL or instance offline?)",
            )
        return try {
            val json = JsonParser.parseString(jsonBody).asJsonObject
            parseLingvaObject(json, httpCode)
        } catch (e: JsonSyntaxException) {
            ProviderResult.Failure(FailureKind.PARSE_ERROR, e.message)
        } catch (e: Exception) {
            ProviderResult.Failure(FailureKind.PARSE_ERROR, e.message)
        }
    }

    private fun parseGraphQlJson(body: String): ProviderResult {
        val jsonBody = prepareJsonBody(body) ?: return ProviderResult.Failure(FailureKind.PARSE_ERROR, "GraphQL response not JSON")
        return try {
            val root = JsonParser.parseString(jsonBody).asJsonObject
            root.getAsJsonArray("errors")?.firstOrNull()?.asJsonObject?.get("message")?.asString?.let {
                return ProviderResult.Failure(FailureKind.BAD_REQUEST, it)
            }
            return when (val data = root.get("data")) {
                null, -> ProviderResult.Failure(FailureKind.BAD_REQUEST, "GraphQL response missing data")
                else -> when {
                    data.isJsonNull -> ProviderResult.Failure(FailureKind.BAD_REQUEST, "GraphQL returned null data")
                    data.isJsonArray -> {
                        if (data.asJsonArray.size() == 0) {
                            ProviderResult.Failure(FailureKind.BAD_REQUEST, "empty data array (scraper unavailable)")
                        } else {
                            ProviderResult.Failure(FailureKind.PARSE_ERROR, "unexpected GraphQL data array")
                        }
                    }
                    data.isJsonObject -> parseGraphQlTranslation(data.asJsonObject)
                    else -> ProviderResult.Failure(FailureKind.PARSE_ERROR, "unexpected GraphQL data type")
                }
            }
        } catch (e: Exception) {
            ProviderResult.Failure(FailureKind.PARSE_ERROR, e.message)
        }
    }

    private fun parseGraphQlTranslation(data: JsonObject): ProviderResult {
        val translation = data.getAsJsonObject("translation")
            ?: return ProviderResult.Failure(FailureKind.BAD_REQUEST, "GraphQL response missing translation")
        val translated = translation.getAsJsonObject("target")?.get("text")?.asString
            ?: return ProviderResult.Failure(FailureKind.BAD_REQUEST, "GraphQL response missing target text")
        val detectedCode = translation.getAsJsonObject("source")?.getAsJsonObject("detected")?.get("code")?.asString
        val sourceLang = detectedCode?.let { LanguageManager.findLanguageFromProviderCode(backend, it) }
        return ProviderResult.Success(translated, sourceLang)
    }

    private fun parseLingvaObject(json: JsonObject, httpCode: Int): ProviderResult {
        failureForBrokenData(json)?.let { return it }
        val error = json.get("error")
        if (error != null && !error.isJsonNull) {
            return ProviderResult.Failure(FailureKind.BAD_REQUEST, readJsonString(error))
        }
        if (json.has("languages")) {
            return ProviderResult.Failure(
                FailureKind.BAD_REQUEST,
                "Lingva returned a language list instead of a translation; set instance URL to the site root (e.g. https://lingva.ml)",
            )
        }
        if (json.has("audio")) {
            return ProviderResult.Failure(FailureKind.BAD_REQUEST, "Lingva returned audio data instead of a translation")
        }
        val translated = readTranslationText(json)
        if (translated == null) {
            ChatPlus.LOGGER.debug("Lingva REST response without translation (HTTP {}): {}", httpCode, previewBody(json))
            return ProviderResult.Failure(
                FailureKind.PARSE_ERROR,
                "Missing translation field",
            )
        }
        val sourceLang = resolveDetectedLanguage(json)
        if (httpCode != 200) {
            return ProviderResult.Failure(FailureKind.BAD_REQUEST, "HTTP $httpCode")
        }
        return ProviderResult.Success(translated, sourceLang)
    }

    private fun failureForBrokenData(json: JsonObject): ProviderResult.Failure? {
        val data = json.get("data") ?: return null
        if (data.isJsonArray && data.asJsonArray.isEmpty()) {
            return ProviderResult.Failure(
                FailureKind.BAD_REQUEST,
                "empty data array (instance scraper unavailable)",
            )
        }
        if (data.isJsonNull) {
            return ProviderResult.Failure(FailureKind.BAD_REQUEST, "null data field")
        }
        return null
    }

    private fun readTranslationText(json: JsonObject): String? {
        readTranslationElement(json.get("translation"))?.let { return it }
        readTranslationElement(json.get("result"))?.let { return it }
        json.get("data")?.takeIf { it.isJsonObject }?.asJsonObject?.let { data ->
            readTranslationElement(data.get("translation"))?.let { return it }
            readTranslationElement(data.get("target"))?.let { return it }
        }
        readTranslationElement(json.get("translatedText"))?.let { return it }
        readTranslationElement(json.get("text"))?.let { return it }
        readTranslationElement(json.getAsJsonObject("target")?.get("text"))?.let { return it }
        return null
    }

    private fun readTranslationElement(element: JsonElement?): String? {
        if (element == null || element.isJsonNull) {
            return null
        }
        if (element.isJsonPrimitive) {
            val value = element.asString
            return value.takeIf { it.isNotBlank() }
        }
        if (element.isJsonArray) {
            val joined = element.asJsonArray.joinToString(" ") { part ->
                when {
                    part.isJsonPrimitive -> part.asString
                    part.isJsonObject -> part.asJsonObject.get("text")?.asString ?: ""
                    else -> ""
                }
            }.trim()
            return joined.takeIf { it.isNotBlank() }
        }
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            return readTranslationElement(obj.get("text"))
                ?: readTranslationElement(obj.get("translation"))
        }
        return null
    }

    private fun readJsonString(element: JsonElement): String {
        return if (element.isJsonPrimitive) {
            element.asString
        } else {
            element.toString()
        }
    }

    private fun resolveDetectedLanguage(json: JsonObject): Language? {
        val info = json.getAsJsonObject("info") ?: return null
        val detected = info.get("detectedSource")
            ?: info.getAsJsonObject("detected")
            ?: return null
        val code = when {
            detected.isJsonPrimitive -> detected.asString
            detected.isJsonObject -> detected.asJsonObject.get("code")?.asString
            else -> null
        } ?: return null
        return LanguageManager.findLanguageFromProviderCode(backend, code)
    }

    private fun previewBody(json: JsonObject): String {
        val keys = json.keySet().joinToString(", ")
        return "keys=[$keys]"
    }

    private fun prepareJsonBody(body: String): String? {
        var trimmed = body.trim()
        if (trimmed.startsWith(")]}'")) {
            trimmed = trimmed.substringAfter('\n', "").trim()
        }
        if (!looksLikeJson(trimmed)) {
            return null
        }
        return trimmed
    }

    private fun looksLikeJson(body: String): Boolean {
        val trimmed = body.trimStart()
        return trimmed.startsWith("{") || trimmed.startsWith("[")
    }

    private fun normalizeBaseUrl(raw: String): String {
        var base = raw.trim().trimEnd('/')
        base = base.replace(Regex("""/api/v1/(languages|translate|audio)$"""), "")
        if (base.endsWith("/api/v1")) {
            base = base.removeSuffix("/api/v1")
        }
        return base.trimEnd('/')
    }

    /** RFC 3986 path-segment encoding (avoids URLEncoder '+' which breaks Lingva routes). */
    private fun encodePathSegment(value: String): String {
        val sb = StringBuilder(value.length)
        value.toByteArray(StandardCharsets.UTF_8).forEach { byte ->
            val code = byte.toInt() and 0xFF
            when (code) {
                in 'a'.code..'z'.code,
                in 'A'.code..'Z'.code,
                in '0'.code..'9'.code,
                '-'.code,
                '_'.code,
                '.'.code,
                '~'.code,
                -> sb.append(code.toChar())
                else -> sb.append(String.format("%%%02X", code))
            }
        }
        return sb.toString()
    }
}