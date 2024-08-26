package com.ebicep.chatplus.features.internal

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatTabGetMessageAtEvent
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.drawString0
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import net.minecraft.client.Minecraft
import kotlin.math.roundToInt

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
            val mouseX = ChatPlusScreen.lastMouseX
            val mouseY = ChatPlusScreen.lastMouseY
            pose.createPose {
                pose.guiForward(amount = 5000.0)
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    "$mouseX,$mouseY",
                    mouseX + 5,
                    mouseY + 5,
                    0xFFFFFF
                )
                val globalSelectedTab = ChatManager.globalSelectedTab
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    "${globalSelectedTab.chatScrollbarPos}",
                    mouseX + 5,
                    mouseY + 15,
                    0xFFFFFF
                )
                // mouse relative to chat window position

                val messageAtEvent = EventBus.post(
                    ChatTabGetMessageAtEvent(
                        globalSelectedTab.chatWindow,
                        globalSelectedTab,
                        globalSelectedTab.screenToChatX(mouseX.toDouble()),
                        globalSelectedTab.screenToChatY(mouseY.toDouble())
                    )
                )
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    "${messageAtEvent.chatX.roundToInt()},${messageAtEvent.chatY.roundToInt()}",
                    mouseX + 5,
                    mouseY - 5,
                    0xFF00FF
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
            val renderer = it.chatWindow.renderer
            val guiGraphics = it.guiGraphics
            val pose = guiGraphics.pose()
            pose.createPose {
                pose.translate0(z = 5000)
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    "${renderer.x},${renderer.y}",
                    renderer.x,
                    renderer.y + 5,
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
            val renderer = it.chatWindow.renderer
            val guiGraphics = it.guiGraphics
            val pose = guiGraphics.pose()
            pose.createPose {
                pose.translate0(z = 5000)
                guiGraphics.drawString0(
                    Minecraft.getInstance().font,
                    "${renderer.rescaledEndX},${renderer.rescaledY - it.displayMessageIndex * renderer.lineHeight}",
                    renderer.rescaledEndX,
                    renderer.rescaledY - it.displayMessageIndex * renderer.lineHeight - 10,
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
            val renderer = it.chatWindow.renderer
            val guiGraphics = it.guiGraphics
            val pose = guiGraphics.pose()
//            pose.createPose {
//                pose.translate0(z = 5000)
//                guiGraphics.drawString(
//                    Minecraft.getInstance().font,
//                    "${it.index}",
//                    renderer.rescaledEndX + 1,
//                    it.verticalChatOffset - renderer.lineHeight,
//                    0x00FF00
//                )
//            }
//            pose.createPose {
//                pose.translate0(x = 20, z = 5000)
//                val scale = .5f
//                val inverseScale = 1 / scale
//                pose.scale(scale, scale, scale)
//                guiGraphics.drawString(
//                    Minecraft.getInstance().font,
//                    it.text.replace("§", "&"),
//                    ((renderer.rescaledEndX + 1) * inverseScale).toInt(),
//                    ((it.verticalChatOffset - renderer.lineHeight) * inverseScale).toInt(),
//                    0xFFFF00
//                )
//            }
        }
    }

}