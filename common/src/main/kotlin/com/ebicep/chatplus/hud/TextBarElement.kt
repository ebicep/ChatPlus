package com.ebicep.chatplus.hud

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.translator.languageSpeakEnabled
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

interface TextBarElement {

    fun getWidth(): Int

    fun getPaddedWidth(): Int {
        return getWidth() + PADDING * 2
    }

    fun getText(): String?

    fun onClick()

    fun onRender(
        guiGraphics: GuiGraphics,
        currentX: Int,
        currentY: Int,
        pMouseX: Int,
        pMouseY: Int,
        pPartialTick: Float
    )

    fun fill(guiGraphics: GuiGraphics, currentX: Int, currentY: Int) {
        guiGraphics.fill(
            currentX,
            currentY,
            currentX + getPaddedWidth(),
            currentY + EDIT_BOX_HEIGHT,
            Minecraft.getInstance().options.getBackgroundColor(Int.MIN_VALUE)
        )
    }

    fun drawCenteredString(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, enabledColor: Int) {
        getText()?.let {
            guiGraphics.drawCenteredString(
                Minecraft.getInstance().font,
                it,
                currentX + getPaddedWidth() / 2,
                currentY + EDIT_BOX_HEIGHT / 4,
                if (findEnabled) enabledColor else 0xFFFFFF
            )
        }
    }

    fun renderOutline(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, color: Int) {
        guiGraphics.renderOutline(
            currentX,
            currentY,
            getPaddedWidth(),
            EDIT_BOX_HEIGHT - 1,
            color
        )
    }

}

class FindTextBarElement(private val chatPlusScreen: ChatPlusScreen) : TextBarElement {

    override fun getWidth(): Int {
        return Minecraft.getInstance().font.width("F")
    }

    override fun getText(): String {
        return "F"
    }

    override fun onClick() {
        findEnabled = !findEnabled
        if (findEnabled) {
            ChatManager.selectedTab.refreshDisplayedMessage(chatPlusScreen.input?.value)
            languageSpeakEnabled = false
        } else {
            ChatManager.selectedTab.refreshDisplayedMessage()
        }
    }

    override fun onRender(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        fill(guiGraphics, currentX, currentY)
        drawCenteredString(guiGraphics, currentX, currentY, 0xFFFF55)
        if (findEnabled) {
            renderOutline(guiGraphics, currentX, currentY, (0xFFFFFF55).toInt())
        }
    }

}

class TranslateSpeakTextBarElement : TextBarElement {

    override fun getWidth(): Int {
        return Minecraft.getInstance().font.width(Config.values.translateSpeak)
    }

    override fun getText(): String {
        return Config.values.translateSpeak
    }

    override fun onClick() {
        languageSpeakEnabled = !languageSpeakEnabled
        if (languageSpeakEnabled) {
            findEnabled = false
        }
    }

    override fun onRender(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        fill(guiGraphics, currentX, currentY)
        drawCenteredString(guiGraphics, currentX, currentY, 0x55FF55)
        if (languageSpeakEnabled) {
            renderOutline(guiGraphics, currentX, currentY, (0xFF55FF55).toInt())
        }
    }

}