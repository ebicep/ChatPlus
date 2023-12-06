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

    fun drawCenteredString(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, color: Int) {
        getText()?.let {
            guiGraphics.drawCenteredString(
                Minecraft.getInstance().font,
                it,
                currentX + getPaddedWidth() / 2,
                currentY + EDIT_BOX_HEIGHT / 4,
                color
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
        chatPlusScreen.rebuildWidgets0()
    }

    override fun onRender(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        fill(guiGraphics, currentX, currentY)
        drawCenteredString(guiGraphics, currentX, currentY, if (findEnabled) 0xFFFF55 else 0xFFFFFF)
        if (findEnabled) {
            renderOutline(guiGraphics, currentX, currentY, (0xFFFFFF55).toInt())
        }
    }

}

class TranslateSpeakTextBarElement(private val chatPlusScreen: ChatPlusScreen) : TextBarElement {

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
        chatPlusScreen.initial = chatPlusScreen.input!!.value
        chatPlusScreen.rebuildWidgets0()
    }

    override fun onRender(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        fill(guiGraphics, currentX, currentY)
        drawCenteredString(guiGraphics, currentX, currentY, if (languageSpeakEnabled) 0x55FF55 else 0xFFFFFF)
        if (languageSpeakEnabled) {
            renderOutline(guiGraphics, currentX, currentY, (0xFF55FF55).toInt())
        }
    }

}