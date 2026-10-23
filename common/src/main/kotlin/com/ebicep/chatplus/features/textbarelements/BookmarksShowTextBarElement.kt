package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.features.BookmarkMessages
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import com.ebicep.chatplus.util.KeyUtil.isLeftMouseButton
import net.minecraft.client.gui.screens.ChatScreen

class ShowBookmarksBarElement(private val chatPlusScreen: ChatScreen) : TextBarElement {

    override fun getWidth(): Int {
        return Minecraft.getInstance().font.width("B")
    }

    override fun getText(): String {
        return "B"
    }

    override fun onClick(button: Int) {
        if (!isLeftMouseButton(button)) {
            return
        }
        BookmarkMessages.toggle(chatPlusScreen)
    }

    override fun onHover(guiGraphics: GuiGraphicsExtractor, pMouseX: Int, pMouseY: Int) {
        guiGraphics.setTooltipForNextFrame(
            chatPlusScreen.font,
            tooltip("chatPlus.bookmark.textBarElement"),
            pMouseX,
            pMouseY
        )
    }

    override fun onRender(guiGraphics: GuiGraphicsExtractor, currentX: Int, currentY: Int, mouseX: Int, mouseY: Int, partialTick: Float) {
        fill(guiGraphics, currentX, currentY)
        drawCenteredString(guiGraphics, currentX, currentY, if (BookmarkMessages.showingBookmarks) Config.values.bookmarkColor else -1)
        if (BookmarkMessages.showingBookmarks) {
            renderOutline(guiGraphics, currentX, currentY, Config.values.bookmarkColor)
        }
    }

}

data class ShowBookmarksToggleEvent(val enabled: Boolean) : Event
