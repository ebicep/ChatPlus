package com.ebicep.chatplus

import dev.architectury.injectables.annotations.ExpectPlatform

object ChatPlusPlatformInit {

    @JvmStatic
    @ExpectPlatform
    fun platformInit() {
        throw AssertionError()
    }
}