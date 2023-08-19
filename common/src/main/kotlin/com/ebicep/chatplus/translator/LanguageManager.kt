package com.ebicep.chatplus.translator

import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*

object LanguageManager {

    private var languages: List<Language> = ArrayList()
    val autoLang: Language

    init {
        val inputStream = LanguageManager::class.java.getResourceAsStream("/assets/chatplus/lang.json")
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val gson = Gson()
        val langArray: Array<Language> = gson.fromJson(reader, Array<Language>::class.java)
        languages = listOf(*langArray)
        autoLang = findLanguageFromGoogle("auto")!! //TODO
    }

    fun findLanguageFromName(name: String): Language? {
        for (lang in languages) {
            if (lang.name == name) {
                return lang
            }
        }
        return null
    }

    fun findLanguageFromGoogle(googleCode: String): Language? {
        for (lang in languages) {
            if (lang.googleCode == googleCode) {
                return lang
            }
        }
        return null
    }

}