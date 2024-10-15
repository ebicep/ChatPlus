package com.ebicep.chatplus

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus.post
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.FeatureManager
import com.ebicep.chatplus.features.chattabs.AddNewMessageEvent
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.SkipNewMessageEvent
import com.ebicep.chatplus.hud.ChatManager.globalSortedTabs
import com.ebicep.chatplus.translator.LanguageManager
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
        val component = Component.literal("From [VIP] sumSmash: hello")
        val addMessagesTo: MutableList<ChatTab> = ArrayList()
        var lastPriority: Int? = null
        for (chatTab in globalSortedTabs) {
            val priority = chatTab.priority
            val alwaysAdd = chatTab.alwaysAdd
            if (lastPriority != null && lastPriority > priority && !alwaysAdd) {
                continue
            }
            if (chatTab.matches(component.string)) {
                addMessagesTo.add(chatTab)
                if (chatTab.skipOthers) {
                    break
                }
                if (!alwaysAdd) {
                    lastPriority = priority
                }
            }
        }
        if (!addMessagesTo.isEmpty()) {
            val messageEvent = AddNewMessageEvent(
                component.copy(),
                component,
                null,
                null,
                Minecraft.getInstance().gui.guiTicks,
                null,
            )
            post(AddNewMessageEvent::class.java, messageEvent)
            if (messageEvent.returnFunction) {
                return
            }
            for (chatTab in addMessagesTo) {
                chatTab.addNewMessage(messageEvent)
            }
        } else {
            val messageEvent = SkipNewMessageEvent(
                component.copy(),
                component,
                null,
                null,
                Minecraft.getInstance().gui.guiTicks,
                null,
            )
            post(SkipNewMessageEvent::class.java, messageEvent)
        }
        for (i in 1..500_000) {
//            ChatManager.globalSelectedTab.addNewMessage(Component.literal("Test $i"), null, 0, null)
        }
    }

    fun isEnabled(): Boolean {
        return Config.values.enabled
    }

    fun sendMessage(component: Component) {
        // rgb(18, 227, 219)
        Minecraft.getInstance().player?.sendSystemMessage(
            Component.literal("ChatPlus").withStyle {
                it.withColor(MOD_COLOR)
            }.append(Component.literal(" > ").withStyle {
                it.withColor(ChatFormatting.DARK_GRAY)
            }).append(component)
        )
    }
}