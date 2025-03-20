package com.ebicep.chatplus

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.internal.FeatureManager
import com.ebicep.chatplus.translator.LanguageManager
import com.ebicep.chatplus.util.ComponentUtil.withColor
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

const val MOD_ID = "chatplus"
const val MOD_COLOR = 0xFF12e3DB.toInt()

object ChatPlus {

    val LOGGER: Logger = LogManager.getLogger(MOD_ID)
    var initialized: Boolean = false

    fun init() {
        initialized = true
        LOGGER.info("Initializing ChatPlus")
        LanguageManager
        Config.load()

        Events
        FeatureManager
        LOGGER.info("Done Initializing ChatPlus")
    }

    fun doTest() {
        Minecraft.getInstance().player!!.connection.sendChat("                 X       Y")
    }

    fun isEnabled(): Boolean {
        return Config.values.enabled
    }

    fun sendMessage(component: Component) {
        // rgb(18, 227, 219)
        Minecraft.getInstance().player?.sendSystemMessage(
            Component.empty()
                .append(Component.literal("ChatPlus").withColor(MOD_COLOR))
                .append(Component.literal(" > ").withStyle(ChatFormatting.DARK_GRAY))
                .append(component)
        )
    }
}