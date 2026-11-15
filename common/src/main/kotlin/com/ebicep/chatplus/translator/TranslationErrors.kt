package com.ebicep.chatplus.translator

import com.ebicep.chatplus.ChatPlus

object TranslationErrors {

    fun log(backend: TranslationBackend, failure: ProviderResult.Failure) {
        val detailSuffix = failure.detail?.let { ": $it" } ?: ""
        when (failure.kind) {
            FailureKind.NETWORK -> ChatPlus.LOGGER.error("[{}] Cannot connect to translation server{}", backend, detailSuffix)
            FailureKind.RATE_LIMITED -> ChatPlus.LOGGER.warn("[{}] Rate limited; pausing this backend for 5 minutes{}", backend, detailSuffix)
            FailureKind.INVALID_KEY -> ChatPlus.LOGGER.error("[{}] API key invalid{}", backend, detailSuffix)
            FailureKind.QUOTA -> ChatPlus.LOGGER.error("[{}] API quota exceeded{}", backend, detailSuffix)
            FailureKind.UNSUPPORTED_LANGUAGE -> ChatPlus.LOGGER.debug("[{}] Unsupported language{}", backend, detailSuffix)
            FailureKind.BAD_REQUEST -> ChatPlus.LOGGER.error("[{}] Bad request{}", backend, detailSuffix)
            FailureKind.PARSE_ERROR -> ChatPlus.LOGGER.error("[{}] Failed to parse response{}", backend, detailSuffix)
            FailureKind.PROVIDER_DISABLED -> ChatPlus.LOGGER.warn("[{}] Provider disabled{}", backend, detailSuffix)
            FailureKind.MISSING_CREDENTIALS -> ChatPlus.LOGGER.warn("[{}] Missing required API credentials{}", backend, detailSuffix)
        }
    }
}