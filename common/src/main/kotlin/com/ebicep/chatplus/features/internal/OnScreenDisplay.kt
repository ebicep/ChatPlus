package com.ebicep.chatplus.features.internal

import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import dev.architectury.event.events.client.ClientGuiEvent
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object OnScreenDisplay {

    init {
        // show on screen
        ClientGuiEvent.RENDER_HUD.register { guiGraphics, _ ->
            val displayEvent = EventBus.post(OnScreenDisplayEvent())
            val components = displayEvent.components
            if (components.isEmpty()) {
                return@register
            }
            val poseStack = guiGraphics.pose()
            components.forEachIndexed { index, it ->
                poseStack.createPose {
                    poseStack.guiForward(amount = 5000.0)
                    poseStack.translate0(y = index * 10.0)
                    guiGraphics.drawCenteredString(
                        Minecraft.getInstance().font,
                        it,
                        Minecraft.getInstance().window.guiScaledWidth / 2,
                        40,
                        0xFFFFFF
                    )
                }
            }
        }
    }

}

data class OnScreenDisplayEvent(val components: MutableList<Component> = mutableListOf()) : Event
