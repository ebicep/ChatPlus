package com.ebicep.chatplus.translator

import com.ebicep.chatplus.ChatPlus
import java.io.IOException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.net.HttpURLConnection

object TranslationHttp {

    data class HttpResponse(val responseCode: Int, val body: String)

    private const val CONNECT_TIMEOUT_MS = 5000
    private const val READ_TIMEOUT_MS = 15000

    fun get(url: String, headers: Map<String, String> = emptyMap()): HttpResponse =
        request("GET", url, null, null, headers)

    fun post(url: String, body: String, contentType: String, headers: Map<String, String> = emptyMap()): HttpResponse =
        request("POST", url, body, contentType, headers)

    fun postForm(url: String, form: String, headers: Map<String, String> = emptyMap()): HttpResponse =
        request("POST", url, form, "application/x-www-form-urlencoded", headers)

    private fun request(
        method: String,
        url: String,
        body: String?,
        contentType: String?,
        headers: Map<String, String>,
    ): HttpResponse {
        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doInput = true
                headers.forEach { (key, value) -> setRequestProperty(key, value) }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", contentType ?: "application/octet-stream")
                    outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                }
                connect()
            }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
            HttpResponse(connection.responseCode, responseBody)
        } catch (e: IOException) {
            ChatPlus.LOGGER.debug("Translation HTTP connection failed: {}", e.message)
            HttpResponse(0, e.message ?: "Connection error")
        } catch (e: Exception) {
            ChatPlus.LOGGER.error(e)
            HttpResponse(0, e.message ?: "Unexpected error")
        }
    }
}