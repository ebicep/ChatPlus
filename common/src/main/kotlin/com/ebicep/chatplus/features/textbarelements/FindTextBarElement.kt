package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.features.FindMessage
import com.ebicep.chatplus.util.KeyUtil.isLeftMouseButton
import com.ebicep.chatplus.util.KeyUtil.isRightMouseButton
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen

class FindTextBarElement(private val chatPlusScreen: ChatScreen) : TextBarElement {

    override fun getWidth(): Int {
        return Minecraft.getInstance().font.width("F")
    }

    override fun getText(): String {
        return "F"
    }

    override fun onClick(button: Int) {
        if (isLeftMouseButton(button)) {
            FindMessage.toggle(chatPlusScreen)
        } else if (isRightMouseButton(button)) {
            FindMessage.toggle(chatPlusScreen, FindMessage.getOtherMode())
        }
    }

    override fun onHover(guiGraphics: GuiGraphicsExtractor, pMouseX: Int, pMouseY: Int) {
        guiGraphics.setTooltipForNextFrame(
            chatPlusScreen.font,
            tooltip("chatPlus.findMessage.highlightInputBox.tooltip"),
            pMouseX,
            pMouseY
        )
    }

    override fun onRender(guiGraphics: GuiGraphicsExtractor, currentX: Int, currentY: Int, mouseX: Int, mouseY: Int, partialTick: Float) {
        fill(guiGraphics, currentX, currentY)
        drawCenteredString(guiGraphics, currentX, currentY, if (FindMessage.findEnabled) FindMessage.findMode!!.color else -1)
        if (FindMessage.findEnabled) {
            renderOutline(guiGraphics, currentX, currentY, FindMessage.findMode!!.color)
        }
    }

}

data class FindToggleEvent(val enabled: Boolean) : Event
