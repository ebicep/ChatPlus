package com.ebicep.chatplus.forge


import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.MOD_ID
import dev.architectury.platform.forge.EventBuses
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.MOD_BUS


@Mod(MOD_ID)
object ChatPlusForge {

    init {
        EventBuses.registerModEventBus(MOD_ID, MOD_BUS)
        ChatPlus.init()

    }

}