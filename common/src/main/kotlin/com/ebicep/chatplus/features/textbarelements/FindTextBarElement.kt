package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.features.FindMessage
import com.ebicep.chatplus.mixin.IMixinScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.network.chat.Component

class FindTextBarElement(private val chatPlusScreen: ChatScreen) : TextBarElement {

    override fun getWidth(): Int {
        return Minecraft.getInstance().font.width("F")
    }

    override fun getText(): String {
        return "F"
    }

    override fun onClick() {
        FindMessage.toggle(chatPlusScreen)
    }

    override fun onHover(guiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int) {
        guiGraphics.renderTooltip(
            (chatPlusScreen as IMixinScreen).font,
            Component.translatable("chatPlus.findMessage.highlightInputBox.tooltip"),
            pMouseX,
            pMouseY
        )
    }

    override fun onRender(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, mouseX: Int, mouseY: Int) {
        fill(guiGraphics, currentX, currentY)
        drawCenteredString(guiGraphics, currentX, currentY, if (FindMessage.findEnabled) FindMessage.FIND_COLOR else -1)
        if (FindMessage.findEnabled) {
            renderOutline(guiGraphics, currentX, currentY, FindMessage.FIND_COLOR)
        }
    }

}

data class FindToggleEvent(val enabled: Boolean) : Event
