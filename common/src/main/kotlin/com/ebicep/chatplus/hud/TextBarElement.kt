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

}

class FindTextBarElement() : TextBarElement {

    override fun getWidth(): Int {
        return Minecraft.getInstance().font.width("F")
    }

    override fun getText(): String {
        return "F"
    }

    override fun onClick() {
        findEnabled = !findEnabled
        if (findEnabled) {
            val screen = Minecraft.getInstance().screen
            if (screen is ChatPlusScreen) {
                ChatManager.selectedTab.refreshDisplayedMessage(screen.input?.value)
            }
            languageSpeakEnabled = false
        } else {
            ChatManager.selectedTab.refreshDisplayedMessage()
        }
    }

    override fun onRender(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        guiGraphics.fill(
            currentX,
            currentY,
            currentX + getPaddedWidth(),
            guiGraphics.guiHeight(),
            Minecraft.getInstance().options.getBackgroundColor(Int.MIN_VALUE)
        )
        guiGraphics.drawCenteredString(
            Minecraft.getInstance().font,
            "F",
            currentX + getPaddedWidth() / 2,
            currentY + EDIT_BOX_HEIGHT / 4,
            if (findEnabled) 0xFFFF55 else 0xFFFFFF // yellow // if enabled
        )
        if (findEnabled)
            guiGraphics.renderOutline(
                currentX,
                guiGraphics.guiHeight() - EDIT_BOX_HEIGHT,
                getPaddedWidth(),
                EDIT_BOX_HEIGHT - 1,
                (0xFFFFFF55).toInt()
            )
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
        guiGraphics.fill(
            currentX,
            currentY,
            currentX + getPaddedWidth(),
            guiGraphics.guiHeight(),
            Minecraft.getInstance().options.getBackgroundColor(Int.MIN_VALUE)
        )
        guiGraphics.drawCenteredString(
            Minecraft.getInstance().font,
            Config.values.translateSpeak,
            currentX + getPaddedWidth() / 2,
            currentY + EDIT_BOX_HEIGHT / 4,
            if (languageSpeakEnabled) 0x55FF55 else 0xFFFFFF // green if enabled
        )
        if (languageSpeakEnabled)
            guiGraphics.renderOutline(
                currentX,
                currentY,
                getPaddedWidth(),
                EDIT_BOX_HEIGHT - 1,
                (0xFF55FF55).toInt()
            )
    }

}