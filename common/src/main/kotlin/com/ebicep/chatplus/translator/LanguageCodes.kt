package com.ebicep.chatplus.translator

object LanguageCodes {

    private val targetOverrides: Map<TranslationBackend, Map<String, String>> = mapOf(
        TranslationBackend.DEEPL to mapOf(
            "zh-CN" to "ZH-HANS",
            "zh-TW" to "ZH-HANT",
            "no" to "NB",
        ),
        TranslationBackend.AZURE to mapOf(
            "zh-CN" to "zh-Hans",
            "zh-TW" to "zh-Hant",
            "no" to "nb",
            "tl" to "fil",
        ),
        TranslationBackend.LIBRETRANSLATE to mapOf(
            "zh-CN" to "zh-Hans",
            "zh-TW" to "zh-Hant",
            "no" to "nb",
        ),
        TranslationBackend.LINGVA to mapOf(
            "zh-CN" to "zh",
            "zh-TW" to "zh_HANT",
            "he" to "iw",
            "jv" to "jw",
        ),
    )

    private val sourceOverrides: Map<TranslationBackend, Map<String, String>> = mapOf(
        TranslationBackend.DEEPL to mapOf(
            "zh-CN" to "ZH",
            "zh-TW" to "ZH",
            "no" to "NB",
        ),
        TranslationBackend.LINGVA to mapOf(
            "zh-TW" to "zh",
        ),
    )

    fun isAutoDetect(language: Language): Boolean = language.googleCode == "auto"

    fun sourceCode(language: Language, backend: TranslationBackend): String? {
        if (isAutoDetect(language)) {
            return when (backend) {
                TranslationBackend.BAIDU -> "auto"
                TranslationBackend.MYMEMORY -> "autodetect"
                TranslationBackend.LIBRETRANSLATE -> "auto"
                TranslationBackend.LINGVA -> "auto"
                TranslationBackend.GOOGLE_UNOFFICIAL -> "auto"
                TranslationBackend.AZURE,
                TranslationBackend.DEEPL,
                TranslationBackend.GOOGLE_CLOUD,
                -> null
            }
        }
        sourceOverrides[backend]?.get(language.googleCode)?.let { return it }
        return targetCode(language, backend)
    }

    fun targetCode(language: Language, backend: TranslationBackend): String? {
        if (isAutoDetect(language)) {
            return null
        }
        targetOverrides[backend]?.get(language.googleCode)?.let { return it }
        return when (backend) {
            TranslationBackend.BAIDU -> language.baiduCode
            TranslationBackend.DEEPL -> language.googleCode.uppercase()
            else -> language.googleCode
        }
    }
}