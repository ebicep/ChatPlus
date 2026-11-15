package com.ebicep.chatplus.translator

import kotlinx.serialization.Serializable

@Serializable
data class TranslatorCredentials(
    var googleCloudApiKey: String = "",
    var deeplApiKey: String = "",
    var azureTranslatorKey: String = "",
    var azureTranslatorRegion: String = "",
    var libreTranslateBaseUrl: String = "https://libretranslate.com",
    var libreTranslateApiKey: String = "",
    var lingvaBaseUrl: String = "https://lingva.ml",
    var myMemoryEmail: String = "",
    var baiduAppId: String = "",
    var baiduSecretKey: String = "",
)