package com.ebicep.chatplus.forge


import com.ebicep.chatplus.MOD_ID
import net.minecraftforge.fml.common.Mod


@Mod(MOD_ID)
object ChatPlusForge {

    init {
        ChatPlusPlatformInitImpl.platformInit()
    }

}