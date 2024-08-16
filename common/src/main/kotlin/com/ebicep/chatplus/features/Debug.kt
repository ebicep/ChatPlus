package com.ebicep.chatplus.features

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.hud.ChatManager.selectedTab
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import net.minecraft.client.Minecraft

object Debug {

    var debug = false

    init {
        // show mouse position
        EventBus.register<ChatScreenRenderEvent> {
            if (!debug) {
                return@register
            }
            val guiGraphics = it.guiGraphics
            val pose = guiGraphics.pose()
            pose.createPose {
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    "${ChatPlusScreen.lastMouseX},${ChatPlusScreen.lastMouseY}",
                    ChatPlusScreen.lastMouseX + 5,
                    ChatPlusScreen.lastMouseY + 5,
                    0xFFFFFF
                )
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    "${selectedTab.chatScrollbarPos}",
                    ChatPlusScreen.lastMouseX + 5,
                    ChatPlusScreen.lastMouseY + 15,
                    0xFFFFFF
                )
            }
        }
        // chat box dimensions
        EventBus.register<ChatRenderPreLinesEvent> {
            if (!debug) {
                return@register
            }
            if (!ChatManager.isChatFocused()) {
                return@register
            }
            val guiGraphics = it.guiGraphics
            val pose = guiGraphics.pose()
            pose.createPose {
                pose.translate0(z = 5000)
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    "${ChatRenderer.x},${ChatRenderer.y}",
                    ChatRenderer.x,
                    ChatRenderer.y + 5,
                    0x00FF00
                )
            }
        }
        EventBus.register<ChatRenderPostLinesEvent> {
            if (!debug) {
                return@register
            }
            if (!ChatManager.isChatFocused()) {
                return@register
            }
            val guiGraphics = it.guiGraphics
            val pose = guiGraphics.pose()
            pose.createPose {
                pose.translate0(z = 5000)
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    "${ChatRenderer.rescaledEndX},${ChatRenderer.rescaledY - it.displayMessageIndex * ChatRenderer.lineHeight}",
                    ChatRenderer.rescaledEndX,
                    ChatRenderer.rescaledY - it.displayMessageIndex * ChatRenderer.lineHeight - 10,
                    0x00FF00
                )
            }
        }
        EventBus.register<ChatRenderLineTextEvent> {
            if (!debug) {
                return@register
            }
            if (!ChatManager.isChatFocused()) {
                return@register
            }
            val guiGraphics = it.guiGraphics
            val pose = guiGraphics.pose()
            pose.createPose {
                pose.translate0(z = 5000)
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    "${it.index}",
                    ChatRenderer.rescaledEndX + 1,
                    it.verticalChatOffset - ChatRenderer.lineHeight,
                    0x00FF00
                )
            }
            pose.createPose {
                pose.translate0(x = 20, z = 5000)
                val scale = .5f
                val inverseScale = 1 / scale
                pose.scale(scale, scale, scale)
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    it.text.replace("§", "&"),
                    ((ChatRenderer.rescaledEndX + 1) * inverseScale).toInt(),
                    ((it.verticalChatOffset - ChatRenderer.lineHeight) * inverseScale).toInt(),
                    0xFFFF00
                )
            }
        }
    }

}