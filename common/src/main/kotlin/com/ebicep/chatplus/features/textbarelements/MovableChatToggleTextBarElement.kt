package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.features.MovableChat.MOVABLE_CHAT_COLOR
import com.ebicep.chatplus.mixin.IMixinScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ChatScreen

class MovableChatToggleTextBarElement(private val chatPlusScreen: ChatScreen) : TextBarElement {

    override fun getWidth(): Int {
        return Minecraft.getInstance().font.width("M")
    }

    override fun getText(): String {
        return "M"
    }

    override fun onClick(button: Int) {
        if (button != 0) {
            return
        }
        Config.values.movableChatEnabled = !Config.values.movableChatEnabled
    }

    override fun onHover(guiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int) {
        guiGraphics.renderTooltip(
            (chatPlusScreen as IMixinScreen).font,
            tooltip("chatPlus.movableChat.textBarElement.toggle.tooltip"),
            pMouseX,
            pMouseY
        )
    }

    override fun onRender(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, mouseX: Int, mouseY: Int) {
        fill(guiGraphics, currentX, currentY)
        drawCenteredString(guiGraphics, currentX, currentY, if (Config.values.movableChatEnabled) MOVABLE_CHAT_COLOR else -1)
        if (Config.values.movableChatEnabled) {
            renderOutline(guiGraphics, currentX, currentY, MOVABLE_CHAT_COLOR)
        }
    }

}