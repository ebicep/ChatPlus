package com.ebicep.chatplus.fabric


import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.events.fabric.ClientCommandRegistration
import net.fabricmc.api.ModInitializer


object ChatPlusFabric : ModInitializer {

    override fun onInitialize() {
        ChatPlus.init()
        ClientCommandRegistration.registerCommands()
    }

}
