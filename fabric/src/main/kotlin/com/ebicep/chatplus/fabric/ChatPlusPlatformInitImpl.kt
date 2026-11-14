package com.ebicep.chatplus.fabric

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.events.fabric.ClientCommandRegistration
import com.ebicep.chatplus.features.speechtotext.VoskJnaEncoding

object ChatPlusPlatformInitImpl {

    @JvmStatic
    fun platformInit() {
        VoskJnaEncoding.ensure()
        ChatPlus.init()
        ClientCommandRegistration.registerCommands()
    }

}