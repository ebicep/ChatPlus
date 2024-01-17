package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.FindText.FIND_COLOR
import com.ebicep.chatplus.features.FindText.findEnabled
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatPlusScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

data class FindToggleEvent(
    val enabled: Boolean
) : Event

class FindTextBarElement(private val chatPlusScreen: ChatPlusScreen) : TextBarElement {

    override fun getWidth(): Int {
        return Minecraft.getInstance().font.width("F")
    }

    override fun getText(): String {
        return "F"
    }

    override fun onClick() {
        findEnabled = !findEnabled
        EventBus.post(FindToggleEvent(findEnabled))
        if (findEnabled) {
            ChatManager.selectedTab.refreshDisplayedMessage(chatPlusScreen.input?.value)
        } else {
            ChatManager.selectedTab.refreshDisplayedMessage()
        }
        chatPlusScreen.initial = chatPlusScreen.input!!.value
        chatPlusScreen.rebuildWidgets0()
    }

    override fun onHover(guiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int) {
        guiGraphics.renderTooltip(chatPlusScreen.font(), Component.translatable("chatPlus.chat.find.tooltip"), pMouseX, pMouseY)
    }

    override fun onRender(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, mouseX: Int, mouseY: Int) {
        fill(guiGraphics, currentX, currentY)
        drawCenteredString(guiGraphics, currentX, currentY, (if (findEnabled) FIND_COLOR else 0xFFFFFF).toInt())
        if (findEnabled) {
            renderOutline(guiGraphics, currentX, currentY, FIND_COLOR.toInt())
        }
    }

}