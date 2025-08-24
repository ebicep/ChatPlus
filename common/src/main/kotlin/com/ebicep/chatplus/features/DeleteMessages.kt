package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.EnumTranslatableName
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatScreenInputEvent
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import java.awt.Color

object DeleteMessages {

    val DEFAULT_COLOR = Color(100, 100, 100, 255).rgb

    init {
        var deleteMessageCooldown: Long = -1
        var messageDeleted = false
        EventBus.register<ChatScreenInputEvent>({ 1 }, { messageDeleted }) {
            if (!Config.values.deleteMessageEnabled) {
                return@register
            }
            val canCopyMessage = deleteMessageCooldown < Events.currentTick && Config.values.deleteMessageKey.isDown()
            if (!canCopyMessage) {
                messageDeleted = false
                return@register
            }
            val tab = ChatManager.globalSelectedTab
            val hoveredOverMessage: ChatTab.ChatPlusGuiMessageLine? = tab.getHoveredOverMessageLine()
            val selectedMessages = SelectChat.getAllSelectedMessages()
            if (hoveredOverMessage != null && selectedMessages.isEmpty()) {
                messageDeleted = true
                deleteMessageCooldown = Events.currentTick + 5
                tab.displayedMessages.remove(hoveredOverMessage)
                tab.messages.remove(hoveredOverMessage.linkedMessage)
            } else if (selectedMessages.isNotEmpty()) {
                messageDeleted = true
                deleteMessageCooldown = Events.currentTick + 5
                tab.displayedMessages.removeAll(selectedMessages)
                tab.messages.removeAll(selectedMessages.map { it.linkedMessage }.toSet())
            }
        }
    }

    @Serializable
    enum class F3DMode(key: String) : EnumTranslatableName {
        DISABLED("chatPlus.deleteMessage.f3DMode.disabled"),
        SELECTED_TAB("chatPlus.deleteMessage.f3DMode.selectedTab"),
        SELECTED_WINDOW("chatPlus.deleteMessage.f3DMode.selectedWindow"),
        ALL("chatPlus.deleteMessage.f3DMode.all"),

        ;

        val translatable: Component = Component.translatable(key)

        override fun getTranslatableName(): Component {
            return translatable
        }

    }

    fun f3D() {
        when (Config.values.deleteMessageF3DMode) {
            F3DMode.DISABLED -> {
            }

            F3DMode.SELECTED_TAB -> {
                ChatManager.globalSelectedTab.clear()
            }

            F3DMode.SELECTED_WINDOW -> {
                ChatManager.selectedWindow.tabSettings.tabs.forEach { it.clear() }
            }

            F3DMode.ALL -> {
                Config.values.chatWindows.forEach { window ->
                    window.tabSettings.tabs.forEach { it.clear() }
                }
            }
        }
    }

}