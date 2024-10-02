package com.ebicep.chatplus.fabric

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.events.fabric.ClientCommandRegistration

object ChatPlusPlatformInitImpl {

    @JvmStatic
    fun platformInit() {
        ChatPlus.init()
        ClientCommandRegistration.registerCommands()
    }

}