package com.ebicep.chatplus.neoforge

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.ConfigScreen
import com.ebicep.chatplus.features.speechtotext.VoskJnaEncoding
import net.minecraft.client.gui.screens.Screen
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModLoadingContext
import net.neoforged.neoforge.client.gui.IConfigScreenFactory


object ChatPlusPlatformInitImpl {

    @JvmStatic
    fun platformInit() {
        VoskJnaEncoding.ensure()
        ChatPlus.init()
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory::class.java) {
            IConfigScreenFactory { _: ModContainer, screen: Screen ->
                ConfigScreen.getConfigScreen(screen)
            }
        }
    }
}