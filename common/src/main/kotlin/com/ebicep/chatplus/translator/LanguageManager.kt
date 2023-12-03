package com.ebicep.chatplus.translator

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*

var languages: List<Language> = ArrayList()
var languageTo: Language? = null
var languageSelf: Language? = null
var languageSpeak: Language? = null

object LanguageManager {

    val autoLang: Language

    init {
        ChatPlus.LOGGER.info("Loading languages...")
        val inputStream = LanguageManager::class.java.getResourceAsStream("/assets/chatplus/lang.json")
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val gson = Gson()
        val langArray: Array<Language> = gson.fromJson(reader, Array<Language>::class.java)
        languages = listOf(*langArray)
        autoLang = findLanguageFromGoogle("auto")!! //TODO
        updateTranslateLanguages()
        ChatPlus.LOGGER.info("Loaded ${languages.size} languages")
    }

    fun updateTranslateLanguages() {
        languageTo = findLanguageFromName(Config.values.translateTo)
        languageSelf = findLanguageFromName(Config.values.translateSelf)
        languageSpeak = findLanguageFromName(Config.values.translateSpeak)
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