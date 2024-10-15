package com.ebicep.chatplus.translator

import com.ebicep.chatplus.ChatPlus
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

class GoogleRequester {

    companion object {
        const val BASE_URL = "https://translate.googleapis.com/translate_a/single"
        var accessDenied = false
    }

    fun translateAuto(message: String, to: Language?): RequestResult {
        return performTranslationRequest(message, LanguageManager.autoLang, to!!)
    }

    fun performTranslationRequest(message: String, from: Language, to: Language): RequestResult {
        val encodedMessage = encodeMessage(message) ?: return RequestResult(2, "Failed to encode message", null, null)
        val queryParams = mutableMapOf(
            "client" to "gtx",
            "sl" to from.googleCode,
            "tl" to to.googleCode,
            "dt" to "t",
            "q" to encodedMessage
        )

        return try {
            val response = sendRequest("GET", queryParams, "application/json")
                ?: return RequestResult(1, "Connection error", null, null)

            when (response.responseCode) {
                200 -> processSuccessfulResponse(response, to)
                429 -> handleAccessDenied()
                else -> handleApiError(response)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            RequestResult(1, "Connection error", null, null)
        }
    }

    private fun encodeMessage(message: String): String? {
        return try {
            URLEncoder.encode(message, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            null
        }
    }

    private fun processSuccessfulResponse(response: Response, targetLanguage: Language): RequestResult {
        val gson: Gson = GsonBuilder().setLenient().create()
        val json: JsonArray = gson.fromJson(response.entity, JsonArray::class.java)

        val detectedSource = LanguageManager.findLanguageFromGoogle(json[2].asString)
        val translatedText = json[0].asJsonArray
            .joinToString(" ") { it.asJsonArray[0].asString }

        return RequestResult(200, translatedText, detectedSource, targetLanguage)
    }

    private fun handleAccessDenied(): RequestResult {
        accessDenied = true
        Timeout().start()
        return RequestResult(429, "Access to Google Translate denied", null, null)
    }

    private fun handleApiError(response: Response): RequestResult {
        accessDenied = true
        ChatPlus.LOGGER.error(response.entity)
        return RequestResult(411, "API call error", null, null)
    }

    private fun sendRequest(method: String, queryParams: Map<String, String>, contentType: String): Response? {
        val requestUrl = buildRequestUrl(queryParams)

        return try {
            val connection = createConnection(requestUrl, method, contentType)
            val responseBody = readResponseBody(connection)
            Response(connection.responseCode, responseBody)
        } catch (e: Exception) {
            when (e) {
                is MalformedURLException -> null
                is IOException -> Response(1, "Failed to connect to server")
                else -> {
                    ChatPlus.LOGGER.error(e)
                    Response(1, "Unexpected error: ${e.message}")
                }
            }
        }
    }

    private fun buildRequestUrl(queryParams: Map<String, String>): String {
        return queryParams.entries.joinToString("&", "$BASE_URL?") { (key, value) ->
            "$key=$value"
        }
    }

    private fun createConnection(requestUrl: String, method: String, contentType: String): HttpsURLConnection {
        return (URL(requestUrl).openConnection() as HttpsURLConnection).apply {
            setRequestProperty("Content-Type", contentType)
            doOutput = true
            requestMethod = method
            connectTimeout = 5000
            connect()
        }
    }

    private fun readResponseBody(connection: HttpsURLConnection): String {
        val inputStream = if (connection.responseCode == HttpsURLConnection.HTTP_OK)
            connection.inputStream
        else
            connection.errorStream

        return inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }
}

data class RequestResult(val code: Int, val message: String, val from: Language?, val to: Language?)

data class Response(val responseCode: Int, val entity: String)

class Timeout : Thread() {
    override fun run() {
        try {
            sleep(300000)
            GoogleRequester.accessDenied = false
        } catch (ignored: InterruptedException) {
        }
    }
}