package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.TranslateMessage.languageSpeakEnabled
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.mixin.IMixinScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.network.chat.Component

class TranslateSpeakTextBarElement(private val chatPlusScreen: ChatScreen) : TextBarElement {

    companion object {
        fun toggleTranslateSpeak(chatPlusScreen: ChatScreen) {
            languageSpeakEnabled = !languageSpeakEnabled
            EventBus.post(TranslateToggleEvent(languageSpeakEnabled))
            chatPlusScreen as IMixinChatScreen
            chatPlusScreen.initial = chatPlusScreen.input!!.value
            chatPlusScreen as IMixinScreen
            chatPlusScreen.callRebuildWidgets()
        }
    }

    override fun getWidth(): Int {
        return Minecraft.getInstance().font.width(Config.values.translateSpeak)
    }

    override fun getText(): String {
        return Config.values.translateSpeak
    }

    override fun onClick() {
        toggleTranslateSpeak(chatPlusScreen)
    }

    override fun onHover(guiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int) {
        chatPlusScreen as IMixinScreen
        guiGraphics.renderTooltip(
            chatPlusScreen.font,
            Component.translatable("chatPlus.translator.translateSpeak.chat.tooltip"),
            pMouseX,
            pMouseY
        )
    }

    override fun onRender(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, mouseX: Int, mouseY: Int) {
        fill(guiGraphics, currentX, currentY)
        drawCenteredString(guiGraphics, currentX, currentY, if (languageSpeakEnabled) 0x55FF55 else 0xFFFFFF)
        if (languageSpeakEnabled) {
            renderOutline(guiGraphics, currentX, currentY, (0xFF55FF55).toInt())
        }
    }

}

data class TranslateToggleEvent(val enabled: Boolean) : Event
