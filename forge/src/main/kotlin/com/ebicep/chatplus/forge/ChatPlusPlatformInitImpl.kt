package com.ebicep.chatplus.forge

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.MOD_ID
import com.ebicep.chatplus.config.ConfigScreen
import dev.architectury.platform.forge.EventBuses
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraftforge.client.ConfigScreenHandler
import net.minecraftforge.fml.ModLoadingContext
import thedarkcolour.kotlinforforge.forge.MOD_BUS

object ChatPlusPlatformInitImpl {

    @JvmStatic
    fun platformInit() {
        EventBuses.registerModEventBus(MOD_ID, MOD_BUS)
        ChatPlus.init()
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory::class.java) {
            ConfigScreenHandler.ConfigScreenFactory { _: Minecraft, screen: Screen ->
                ConfigScreen.getConfigScreen(screen)
            }
        }
    }

}