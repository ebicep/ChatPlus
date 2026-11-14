package com.ebicep.chatplus.neoforge

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.ConfigScreen
import com.ebicep.chatplus.features.speechtotext.VoskJnaEncoding
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.neoforged.fml.ModLoadingContext
import net.neoforged.neoforge.client.ConfigScreenHandler


object ChatPlusPlatformInitImpl {

    @JvmStatic
    fun platformInit() {
        VoskJnaEncoding.ensure()
        ChatPlus.init()
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory::class.java) {
            ConfigScreenHandler.ConfigScreenFactory { _: Minecraft, screen: Screen ->
                ConfigScreen.getConfigScreen(screen)
            }
        }
    }
}