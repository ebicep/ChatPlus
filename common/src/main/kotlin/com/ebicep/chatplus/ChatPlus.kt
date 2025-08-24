package com.ebicep.chatplus

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.internal.FeatureManager
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.translator.LanguageManager
import com.ebicep.chatplus.util.ComponentUtil
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
        val component = ComponentUtil.literal("HELLO ", ChatFormatting.RED)
            .append(
                ComponentUtil.literal("WORLD ", ChatFormatting.GREEN)
                    .append(Component.literal("LOL! ").withStyle {
                        it.withColor(0xFF12e5)
                    }).append(Component.literal("HEHE ").append(Component.literal("XD ").withStyle {
//                        it.withBold(true)
                        it
                    }))
            )
            .append(Component.literal("TEST?").withStyle {
                it.withColor(0xA6A6A6)
            })
        ChatManager.globalSelectedTab.updateCurrentSettings()
        println("Final value: ${ChatManager.globalSelectedTab.displayedMessages.last().coloredContent()}")
//        Minecraft.getInstance().player!!.sendSystemMessage(component)
//        Minecraft.getInstance().player!!.connection.sendChat(component.getColoredString())
//        component.visit({ style: Style, string: String ->
//            println("$style - $string")
//            Optional.empty<Any?>()
//        }, Style.EMPTY)
//        println("Final value: ${component.getColoredString()}")
        val component2 = Component.literal("§bYou §ahave §b8 §auncl§laimed §rleveling rewards!").withStyle {
            it.withColor(0xFF12e5)
        }
//        Minecraft.getInstance().player!!.sendSystemMessage(component2)
//        println(component2.getColoredString())
//
//        component2.visit({ style: Style, string: String ->
//            println("$style - $string")
//            Optional.empty<Any?>()
//        }, Style.EMPTY)
//        println()
//        println("Final value: ${component2.getColoredString()}")
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