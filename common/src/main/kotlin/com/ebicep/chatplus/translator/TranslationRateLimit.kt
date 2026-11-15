package com.ebicep.chatplus.translator

class TranslationRateLimit(private val cooldownMs: Long = 300_000L) {

    @Volatile
    private var blockedUntil: Long = 0L

    val blocked: Boolean
        get() = System.currentTimeMillis() < blockedUntil

    fun triggerCooldown() {
        blockedUntil = System.currentTimeMillis() + cooldownMs
    }
}