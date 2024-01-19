package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.ScreenshotChat.SCREENSHOT_COLOR
import com.ebicep.chatplus.features.ScreenshotChat.lastScreenShotTick
import com.ebicep.chatplus.hud.ChatPlusScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

class ScreenShotChatEvent : Event

class ScreenShotChatElement(private val chatPlusScreen: ChatPlusScreen) : TextBarElement {

    override fun getWidth(): Int {
        return Minecraft.getInstance().font.width("S")
    }

    override fun getText(): String {
        return "S"
    }

    override fun onClick() {
        if (onCooldown()) {
            return
        }
        EventBus.post(ScreenShotChatEvent())
    }

    private fun onCooldown() = lastScreenShotTick + 60 > Events.currentTick

    override fun onHover(guiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int) {
        guiGraphics.renderTooltip(chatPlusScreen.font(), Component.translatable("chatPlus.screenShotChat.tooltip"), pMouseX, pMouseY)
    }

    override fun onRender(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, mouseX: Int, mouseY: Int) {
        fill(guiGraphics, currentX, currentY)
        val onCooldown = onCooldown()
        drawCenteredString(guiGraphics, currentX, currentY, (if (onCooldown) SCREENSHOT_COLOR else 0xFFFFFF).toInt())
        if (onCooldown) {
            renderOutline(guiGraphics, currentX, currentY, SCREENSHOT_COLOR.toInt())
        }
    }

}