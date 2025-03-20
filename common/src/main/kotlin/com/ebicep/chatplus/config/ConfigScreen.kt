package com.ebicep.chatplus.config

import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chatwindows.ChatWindow
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

    @JvmStatic
    @ExpectPlatform
    fun getTabEditorScreen(previousScreen: Screen? = null, chatTab: ChatTab): Screen {
        throw AssertionError()
    }

    @JvmStatic
    @ExpectPlatform
    fun getWindowEditorScreen(previousScreen: Screen? = null, chatWindow: ChatWindow): Screen {
        throw AssertionError()
    }

}