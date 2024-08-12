package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.MessageDirection
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatManager.selectedTab
import com.ebicep.chatplus.hud.ChatRenderPreLinesRenderEvent
import com.ebicep.chatplus.hud.ChatRenderer.lineHeight
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import net.minecraft.util.Mth

object Animations {

    init {
        EventBus.register<ChatRenderPreLinesRenderEvent> {
            if (!Config.values.animationEnabled) {
                return@register
            }
            it.guiGraphics.pose().translate0(y = getAnimationMessageTransitionOffset())
        }
    }

    private fun getAnimationMessageTransitionOffset(): Int {
        val timeAlive: Long = System.currentTimeMillis() - selectedTab.lastMessageTime
        val fadeTime = Config.values.animationNewMessageTransitionTime.toFloat()
        if (timeAlive >= fadeTime || selectedTab.chatScrollbarPos != 0) {
            return 0
        }
        val offset = lineHeight - Mth.lerp(timeAlive / fadeTime, 0.0f, lineHeight.toFloat()).toInt()
        return when (Config.values.messageDirection) {
            MessageDirection.TOP_DOWN -> -offset
            MessageDirection.BOTTOM_UP -> offset
        }
    }

}