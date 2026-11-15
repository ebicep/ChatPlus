package com.ebicep.chatplus.translator

import com.ebicep.chatplus.translator.providers.*

object TranslationProviders {

    private val providers: Map<TranslationBackend, TranslationProvider> = mapOf(
        TranslationBackend.GOOGLE_UNOFFICIAL to GoogleUnofficialProvider,
        TranslationBackend.GOOGLE_CLOUD to GoogleCloudProvider,
        TranslationBackend.DEEPL to DeepLProvider,
        TranslationBackend.AZURE to AzureTranslatorProvider,
        TranslationBackend.LIBRETRANSLATE to LibreTranslateProvider,
        TranslationBackend.LINGVA to LingvaProvider,
        TranslationBackend.MYMEMORY to MyMemoryProvider,
        TranslationBackend.BAIDU to BaiduProvider,
    )

    fun get(backend: TranslationBackend): TranslationProvider =
        providers[backend] ?: GoogleUnofficialProvider
}