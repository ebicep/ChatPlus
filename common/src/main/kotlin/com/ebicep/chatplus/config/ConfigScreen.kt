package com.ebicep.chatplus.config

import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

object ConfigScreen {

    var open = false

    fun handleOpenScreen() {
        if (open) {
            open = false
            openConfigScreen()
        }
    }

    private fun openConfigScreen() {
        val screen = getConfigScreen()
        Minecraft.getInstance().setScreen(screen)
    }

    @JvmStatic
    @ExpectPlatform
    fun getConfigScreen(previousScreen: Screen? = null): Screen {
        throw AssertionError()
    }

}