package com.ebicep.chatplus.translator

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets


object LanguageManager {

    var languages: List<Language> = ArrayList()
    var translateFrom: Language? = null
    var languageTo: Language? = null
    var languageSelf: Language? = null
    var languageSpeak: Language? = null
    var languageSpeakEnabled = false

    val autoLang: Language

    init {
        ChatPlus.LOGGER.info("Loading languages...")
        val inputStream = LanguageManager::class.java.getResourceAsStream("/assets/chatplus/lang.json")
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val gson = Gson()
        val langArray: Array<Language> = gson.fromJson(reader, Array<Language>::class.java)
        languages = listOf(*langArray)
        autoLang = findLanguageFromGoogle("auto")!! //TODO
        ChatPlus.LOGGER.info("Loaded ${languages.size} languages")
    }

    fun updateTranslateLanguages() {
        translateFrom = findLanguageFromName(Config.values.translateFrom)
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

    private val providerCodeAliases: Map<String, String> = mapOf(
        "zh" to "zh-CN",
        "zh-hans" to "zh-CN",
        "zh-hant" to "zh-TW",
        "zh_hant" to "zh-TW",
        "zt" to "zh-TW",
        "iw" to "he",
        "jw" to "jv",
        "nb" to "no",
        "fil" to "tl",
    )

    fun findLanguageFromProviderCode(backend: TranslationBackend, code: String): Language? {
        if (code.equals("auto", ignoreCase = true) || code.equals("autodetect", ignoreCase = true)) {
            return autoLang
        }
        if (backend == TranslationBackend.BAIDU) {
            return languages.find { it.baiduCode == code }
        }
        val normalized = code.lowercase()
        providerCodeAliases[normalized]?.let { findLanguageFromGoogle(it) }?.let { return it }
        languages.find { it.googleCode.equals(code, ignoreCase = true) }?.let { return it }
        findLanguageFromGoogle(normalized)?.let { return it }
        val prefix = code.substringBefore('-').substringBefore('_')
        if (prefix.isNotEmpty() && prefix != code) {
            providerCodeAliases[prefix.lowercase()]?.let { findLanguageFromGoogle(it) }?.let { return it }
            languages.find { it.googleCode.equals(prefix, ignoreCase = true) }?.let { return it }
            findLanguageFromGoogle(prefix.lowercase())?.let { return it }
        }
        return null
    }

}