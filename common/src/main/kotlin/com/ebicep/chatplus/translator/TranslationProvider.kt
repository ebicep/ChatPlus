package com.ebicep.chatplus.translator

sealed interface ProviderResult {
    data class Success(val text: String, val detectedSource: Language?) : ProviderResult
    data class Failure(val kind: FailureKind, val detail: String? = null) : ProviderResult
}

enum class FailureKind {
    NETWORK,
    RATE_LIMITED,
    INVALID_KEY,
    QUOTA,
    UNSUPPORTED_LANGUAGE,
    BAD_REQUEST,
    PARSE_ERROR,
    PROVIDER_DISABLED,
    MISSING_CREDENTIALS,
}

interface TranslationProvider {
    val backend: TranslationBackend
    val rateLimit: TranslationRateLimit

    fun hasRequiredCredentials(credentials: TranslatorCredentials): Boolean

    fun translate(text: String, from: Language?, to: Language, credentials: TranslatorCredentials): ProviderResult
}