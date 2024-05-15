package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.features.BookmarkMessages
import com.ebicep.chatplus.hud.ChatPlusScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

data class ShowBookmarksToggleEvent(
    val enabled: Boolean
) : Event

class ShowBookmarksBarElement(private val chatPlusScreen: ChatPlusScreen) : TextBarElement {

    override fun getWidth(): Int {
        return Minecraft.getInstance().font.width("B")
    }

    override fun getText(): String {
        return "B"
    }

    override fun onClick() {
        BookmarkMessages.toggle(chatPlusScreen)
    }

    override fun onHover(guiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int) {
        guiGraphics.renderTooltip(
            chatPlusScreen.font(),
            Component.translatable("chatPlus.bookmark.textBarElement"),
            pMouseX,
            pMouseY
        )
    }

    override fun onRender(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, mouseX: Int, mouseY: Int) {
        fill(guiGraphics, currentX, currentY)
        drawCenteredString(guiGraphics, currentX, currentY, if (BookmarkMessages.showingBoomarks) Config.values.bookmarkColor else -1)
        if (BookmarkMessages.showingBoomarks) {
            renderOutline(guiGraphics, currentX, currentY, Config.values.bookmarkColor)
        }
    }

}