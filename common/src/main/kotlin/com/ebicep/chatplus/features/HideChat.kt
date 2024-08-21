package com.ebicep.chatplus.features

import com.ebicep.chatplus.MOD_COLOR
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.internal.OnScreenDisplayEvent
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderPreLinesEvent
import com.ebicep.chatplus.util.ComponentUtil.withColor
import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientRawInputEvent
import net.minecraft.network.chat.Component

object HideChat {

    private val CHAT_HIDDEN_COMPONENT = Component.literal("Chat Hidden").withColor(MOD_COLOR)

    init {
        var hidden = false
        EventBus.register<ChatRenderPreLinesEvent>({ 200 }, { hidden }) {
            hidden = false
            if (!Config.values.hideChatEnabled) {
                return@register
            }
            if (ChatManager.isChatFocused() && Config.values.hideChatShowWhenFocused) {
                return@register
            }
            it.returnFunction = true
            hidden = true
        }
        var onCooldown = false
        ClientRawInputEvent.KEY_PRESSED.register { _, keyCode, _, action, modifiers ->
            if (ChatManager.isChatFocused()) {
                return@register EventResult.pass()
            }
            if (keyCode != Config.values.hideChatToggleKey.key.value || modifiers != Config.values.hideChatToggleKey.modifier.toInt()) {
                return@register EventResult.pass()
            }
            if (action == 0) { // release
                onCooldown = false
                return@register EventResult.pass()
            }
            if (onCooldown) {
                return@register EventResult.pass()
            }
            onCooldown = true
            Config.values.hideChatEnabled = !Config.values.hideChatEnabled
            EventResult.interruptTrue()
        }
        EventBus.register<OnScreenDisplayEvent> {
            if (!Config.values.hideChatEnabled || !Config.values.hideChatShowHiddenOnScreen) {
                return@register
            }
            it.components.add(CHAT_HIDDEN_COMPONENT)
        }
    }

}