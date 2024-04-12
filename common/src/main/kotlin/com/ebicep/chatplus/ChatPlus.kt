package com.ebicep.chatplus

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.FeatureManager
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.translator.LanguageManager
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

const val MOD_ID = "chatplus"

object ChatPlus {

    val LOGGER: Logger = LogManager.getLogger(MOD_ID)

    fun init() {
        LanguageManager
        Config.load()

        Events
        FeatureManager
    }

    fun doTest() {
        for (i in 1..50) {
            ChatManager.selectedTab.addNewMessage(Component.literal("Test $i"), null, i, null)
        }
    }

    fun isEnabled(): Boolean {
        return Config.values.enabled
    }

    fun sendMessage(component: Component) {
        // rgb(18, 227, 219)
        Minecraft.getInstance().player?.sendSystemMessage(
            Component.literal("ChatPlus").withStyle {
                it.withColor(0xFF12e3DB.toInt())
            }.append(Component.literal(" > ").withStyle {
                it.withColor(ChatFormatting.DARK_GRAY)
            }).append(component)
        )
    }
}