package com.ebicep.chatplus.features

import com.ebicep.chatplus.IChatScreen
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatScreenRenderEvent
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.drawString0
import com.ebicep.chatplus.util.GraphicsUtil.fill0
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import net.minecraft.client.Minecraft

object InputBoxCharacterCount {

    private const val PADDING = 2
    private const val SCALE = 0.5f
    private const val PADDING_SCALED = (PADDING * 2) / SCALE

    init {
        EventBus.register<ChatScreenRenderEvent> {
            if (!Config.values.inputBoxSettings.showInputBoxInputLength) {
                return@register
            }
            val screen = it.screen
            screen as IMixinChatScreen
            screen as IChatScreen
            val input = screen.input.value
            val guiGraphics = it.guiGraphics
            val poseStack = guiGraphics.pose()
            val x = 0
            val y =
                if (Config.values.vanillaInputBox) Minecraft.getInstance().window.guiScaledHeight - 14
                else Config.values.inputBoxSettings.getCalculatedStartY() - MovableChat.InputBoxSettings.INPUT_BOX_PADDING
            poseStack.createPose {
                val length = "${input.length}"
                val width = Minecraft.getInstance().font.width(length)
                poseStack.scale(SCALE, SCALE, 1f)
                poseStack.translate0(x = (Config.values.inputBoxSettings.startX / SCALE + screen.chatPlusWidth / SCALE - width) - PADDING_SCALED, z = 1f)
                guiGraphics.fill0(
                    x / SCALE,
                    y / SCALE,
                    (x / SCALE + width) + PADDING_SCALED,
                    (y / SCALE) + 15,
                    Config.values.inputBoxSettings.showInputBoxInputLengthBackgroundColor
                )
                guiGraphics.drawString0(
                    length,
                    (x + PADDING) / SCALE,
                    (y + PADDING) / SCALE,
                    getTextColor(input.length),
                )
            }
        }
    }

    fun getTextColor(currentLength: Int): Int {
        val min = 0
        val max = Config.values.inputBoxSettings.maxInputBoxInputLength
        val range = max - min
        val percent = (currentLength - min) / range.toFloat()
        return when (percent) {
            in 0f..0.33f -> 0x00FF00 // green
            in 0.33f..0.66f -> 0xFFFF00 // yellow
            in 0.66f..0.99f -> 0xFFA500 // orange
            else -> 0xFF0000 // red
        }
    }

}