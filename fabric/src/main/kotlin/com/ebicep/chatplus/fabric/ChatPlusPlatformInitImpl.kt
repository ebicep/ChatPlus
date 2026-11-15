package com.ebicep.chatplus.fabric

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.fabric.ConfigDirectoryImpl
import com.ebicep.chatplus.config.fabric.ConfigScreenImpl
import com.ebicep.chatplus.events.fabric.ClientCommandRegistration
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.features.speechtotext.VoskJnaEncoding
import com.ebicep.chatplus.platform.PlatformServicesProvider
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.resources.Identifier

class ChatPlusPlatformInitImpl : PlatformServicesProvider {

    private val tickListeners = mutableListOf<() -> Unit>()
    private val hudListeners = mutableListOf<(GuiGraphicsExtractor, Float) -> Unit>()
    private val startedListeners = mutableListOf<() -> Unit>()
    private val stoppingListeners = mutableListOf<() -> Unit>()
    private val levelLoadListeners = mutableListOf<() -> Unit>()
    private var tickHooked = false
    private var hudHooked = false
    private var lifecycleHooked = false

    override fun platformInit() {
        VoskJnaEncoding.ensure()
        ChatPlus.init()
        ClientCommandRegistration.registerCommands()
    }

    override fun getConfigDirectory() = ConfigDirectoryImpl.getConfigDirectory()

    override fun getConfigScreen(previousScreen: Screen?): Screen {
        return ConfigScreenImpl.getConfigScreen(previousScreen)
    }

    override fun getTabEditorScreen(previousScreen: Screen?, chatTab: ChatTab): Screen {
        return ConfigScreenImpl.getTabEditorScreen(previousScreen, chatTab)
    }

    override fun getWindowEditorScreen(previousScreen: Screen?, chatWindow: ChatWindow): Screen {
        return ConfigScreenImpl.getWindowEditorScreen(previousScreen, chatWindow)
    }

    override fun registerClientTickPost(listener: () -> Unit) {
        tickListeners.add(listener)
        ensureTickHook()
    }

    override fun registerRenderHud(listener: (GuiGraphicsExtractor, Float) -> Unit) {
        hudListeners.add(listener)
        ensureHudHook()
    }

    override fun registerClientStarted(listener: () -> Unit) {
        startedListeners.add(listener)
        ensureLifecycleHooks()
    }

    override fun registerClientStopping(listener: () -> Unit) {
        stoppingListeners.add(listener)
        ensureLifecycleHooks()
    }

    override fun registerClientLevelLoad(listener: () -> Unit) {
        levelLoadListeners.add(listener)
        ensureLifecycleHooks()
    }

    private fun ensureTickHook() {
        if (tickHooked) {
            return
        }
        tickHooked = true
        ClientTickEvents.END_CLIENT_TICK.register {
            tickListeners.forEach { it() }
        }
    }

    private fun ensureHudHook() {
        if (hudHooked) {
            return
        }
        hudHooked = true
        // Fabric HUD layers require unique identifiers, so use one element and fan out to listeners.
        val hudElement: HudElement = HudElement { graphics, deltaTracker ->
            val tickDelta = deltaTracker.gameTimeDeltaTicks
            hudListeners.forEach { it(graphics, tickDelta) }
        }
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("chatplus", "hud"),
            hudElement
        )
    }

    private fun ensureLifecycleHooks() {
        if (lifecycleHooked) {
            return
        }
        lifecycleHooked = true
        ClientLifecycleEvents.CLIENT_STARTED.register {
            startedListeners.forEach { it() }
        }
        ClientLifecycleEvents.CLIENT_STOPPING.register {
            stoppingListeners.forEach { it() }
        }
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ ->
            levelLoadListeners.forEach { it() }
        }
    }
}
