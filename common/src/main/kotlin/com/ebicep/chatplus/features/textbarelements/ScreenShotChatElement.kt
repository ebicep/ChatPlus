package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.ScreenshotChat.SCREENSHOT_COLOR
import com.ebicep.chatplus.features.ScreenshotChat.onCooldown
import com.ebicep.chatplus.mixin.IMixinScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.network.chat.Component

class ScreenShotChatElement(private val chatPlusScreen: ChatScreen) : TextBarElement {

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

    override fun onHover(guiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int) {
        guiGraphics.renderTooltip(
            (chatPlusScreen as IMixinScreen).font,
            Component.translatable("chatPlus.screenshotChat.tooltip"),
            pMouseX,
            pMouseY
        )
    }

    override fun onRender(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, mouseX: Int, mouseY: Int) {
        fill(guiGraphics, currentX, currentY)
        val onCooldown = onCooldown()
        drawCenteredString(guiGraphics, currentX, currentY, if (onCooldown) SCREENSHOT_COLOR else 0xFFFFFF)
        if (onCooldown) {
            renderOutline(guiGraphics, currentX, currentY, SCREENSHOT_COLOR)
        }
    }

}

class ScreenShotChatEvent : Event
