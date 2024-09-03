package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.hud.ChatPlusScreen.EDIT_BOX_DISPLAY_HEIGHT
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

interface TextBarElement {

    fun getWidth(): Int

    fun getPaddedWidth(): Int {
        return getWidth() + TextBarElements.PADDING * 2
    }

    fun getText(): String?

    fun onClick()

    fun onHover(guiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int) {

    }

    fun onRender(
        guiGraphics: GuiGraphics,
        currentX: Int,
        currentY: Int,
        mouseX: Int,
        mouseY: Int
    )

    fun fill(guiGraphics: GuiGraphics, currentX: Int, currentY: Int) {
        guiGraphics.fill(
            currentX,
            currentY,
            currentX + getPaddedWidth(),
            currentY + EDIT_BOX_DISPLAY_HEIGHT,
            Minecraft.getInstance().options.getBackgroundColor(Int.MIN_VALUE)
        )
    }

    fun drawCenteredString(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, color: Int) {
        getText()?.let {
            guiGraphics.drawCenteredString(
                Minecraft.getInstance().font,
                it,
                currentX + getPaddedWidth() / 2,
                currentY + EDIT_BOX_DISPLAY_HEIGHT / 4,
                color
            )
        }
    }

    fun renderOutline(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, color: Int) {
        guiGraphics.renderOutline(
            currentX,
            currentY,
            getPaddedWidth(),
            EDIT_BOX_DISPLAY_HEIGHT,
            color
        )
    }

}

