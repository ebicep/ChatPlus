package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.SendNote
import com.ebicep.chatplus.features.SendNote.NOTE_COLOR
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.util.KeyUtil.isLeftMouseButton
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen

class SendNoteTextBarElement(private val chatPlusScreen: ChatScreen) : TextBarElement {

    override fun getWidth(): Int {
        return Minecraft.getInstance().font.width("N")
    }

    override fun getText(): String {
        return "N"
    }

    override fun onClick(button: Int) {
        if (!isLeftMouseButton(button)) {
            return
        }
        chatPlusScreen as IMixinChatScreen
        val input = chatPlusScreen.input.value ?: return
        EventBus.post(SendNoteEvent(input))
    }

    override fun onHover(guiGraphics: GuiGraphicsExtractor, pMouseX: Int, pMouseY: Int) {
        guiGraphics.setTooltipForNextFrame(
            chatPlusScreen.font,
            tooltip("chatPlus.sendNote.textBarElement.tooltip"),
            pMouseX,
            pMouseY
        )
    }

    override fun onRender(guiGraphics: GuiGraphicsExtractor, currentX: Int, currentY: Int, mouseX: Int, mouseY: Int, partialTick: Float) {
        fill(guiGraphics, currentX, currentY)
        val onCooldown = SendNote.onCooldown()
        drawCenteredString(guiGraphics, currentX, currentY, if (onCooldown) NOTE_COLOR else -1)
        if (onCooldown) {
            renderOutline(guiGraphics, currentX, currentY, NOTE_COLOR)
        }
    }

}

data class SendNoteEvent(val message: String) : Event
