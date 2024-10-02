package com.ebicep.chatplus.fabric


import net.fabricmc.api.ModInitializer


object ChatPlusFabric : ModInitializer {

    override fun onInitialize() {
        ChatPlusPlatformInitImpl.platformInit()
    }

}
