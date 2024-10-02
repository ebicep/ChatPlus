package com.ebicep.chatplus.forge

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.MOD_ID
import dev.architectury.platform.forge.EventBuses
import thedarkcolour.kotlinforforge.forge.MOD_BUS

object ChatPlusPlatformInitImpl {

    @JvmStatic
    fun platformInit() {
        EventBuses.registerModEventBus(MOD_ID, MOD_BUS)
        ChatPlus.init()
    }

}