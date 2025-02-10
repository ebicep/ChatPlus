package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.SendNote
import com.ebicep.chatplus.features.SendNote.NOTE_COLOR
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.mixin.IMixinScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ChatScreen

class SendNoteTextBarElement(private val chatPlusScreen: ChatScreen) : TextBarElement {

    override fun getWidth(): Int {
        return Minecraft.getInstance().font.width("N")
    }

    override fun getText(): String {
        return "N"
    }

    override fun onClick(button: Int) {
        if (button != 0) {
            return
        }
        chatPlusScreen as IMixinChatScreen
        val input = chatPlusScreen.input.value ?: return
        EventBus.post(SendNoteEvent(input))
    }

    override fun onHover(guiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int) {
        guiGraphics.renderTooltip(
            (chatPlusScreen as IMixinScreen).font,
            tooltip("chatPlus.sendNote.textBarElement.tooltip"),
            pMouseX,
            pMouseY
        )
    }

    override fun onRender(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, mouseX: Int, mouseY: Int) {
        fill(guiGraphics, currentX, currentY)
        val onCooldown = SendNote.onCooldown()
        drawCenteredString(guiGraphics, currentX, currentY, if (onCooldown) NOTE_COLOR else 0xFFFFFF)
        if (onCooldown) {
            renderOutline(guiGraphics, currentX, currentY, NOTE_COLOR)
        }
    }

}

data class SendNoteEvent(val message: String) : Event
