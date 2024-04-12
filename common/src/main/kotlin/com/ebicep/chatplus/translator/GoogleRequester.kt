package com.ebicep.chatplus.translator

import com.ebicep.chatplus.ChatPlus
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
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

    fun translateAuto(message: String?, to: Language?): RequestResult {
        return translate(message, LanguageManager.autoLang, to!!)
    }

    fun translate(message: String?, from: Language, to: Language): RequestResult {
        val queryParam: MutableMap<String, String?> = HashMap()
        var encodedMessage: String? = null
        try {
            encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.name())
        } catch (ignored: UnsupportedEncodingException) {
        }
        queryParam["client"] = "gtx"
        queryParam["sl"] = from.googleCode
        queryParam["tl"] = to.googleCode
        queryParam["dt"] = "t"
        queryParam["q"] = encodedMessage
        return try {
            val response: Response = sendRequest("GET", queryParam, "application/json")
                ?: return RequestResult(1, "Connection error", null, null)
            if (response.responseCode != 200) {
                return if (response.responseCode == 429) {
                    accessDenied = true
                    val timeout: Thread = Timeout()
                    timeout.start()
                    RequestResult(429, "Access to Google Translate denied", null, null)
                } else {
                    accessDenied = true
                    ChatPlus.LOGGER.error(response.entity)
                    RequestResult(411, "API call error", null, null)
                }
            }
            val responseString: String = response.entity
            val gson: Gson = GsonBuilder().setLenient().create()
            val json: JsonArray = gson.fromJson(responseString, JsonArray::class.java)
            val detectedSource: Language? = LanguageManager.findLanguageFromGoogle(json.get(2).asString)
            val lines: JsonArray = json.get(0).asJsonArray
            val stringBuilder = StringBuilder()
            for (sentenceObj in lines) {
                val sentence: JsonArray = sentenceObj.asJsonArray
                stringBuilder.append(sentence.get(0).asString)
                stringBuilder.append(" ")
            }
            RequestResult(200, stringBuilder.toString(), detectedSource, to)
        } catch (e: Exception) {
            e.printStackTrace()
            RequestResult(1, "Connection error", null, null)
        }
    }

    private fun sendRequest(method: String, queryParams: MutableMap<String, String?>, contentType: String): Response? {
        val requestUrl: StringBuilder = StringBuilder(BASE_URL)
        var firstParam = true
        for (key in queryParams.keys) {
            if (firstParam) {
                requestUrl.append("?")
                firstParam = false
            } else {
                requestUrl.append("&")
            }
            requestUrl.append(key).append("=").append(queryParams[key])
        }
        val connection: HttpsURLConnection
        return try {
            val request = URL(requestUrl.toString())
            connection = request.openConnection() as HttpsURLConnection
            connection.setRequestProperty("Content-Type", contentType)
            connection.doOutput = true
            connection.requestMethod = method
            connection.connectTimeout = 5000
            connection.connect()
            val reader = BufferedReader(
                InputStreamReader(
                    if (connection.responseCode == 200) connection.inputStream else connection.errorStream,
                    StandardCharsets.UTF_8
                )
            )
            val responseString = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                responseString.append(line)
            }
            Response(connection.responseCode, responseString.toString())
        } catch (ignored: MalformedURLException) {
            null
        } catch (e: IOException) {
            Response(1, "Failed to connect to server")
        }
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