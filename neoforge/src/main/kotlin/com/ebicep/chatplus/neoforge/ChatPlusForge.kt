package com.ebicep.chatplus.neoforge


import com.ebicep.chatplus.MOD_ID
import net.neoforged.fml.common.Mod


@Mod(MOD_ID)
object ChatPlusForge {

    init {
        ChatPlusPlatformInitImpl.platformInit()
    }

}