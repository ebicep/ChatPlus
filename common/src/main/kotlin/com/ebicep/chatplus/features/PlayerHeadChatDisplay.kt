package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.ChatPlusMinuteEvent
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatTabAddDisplayMessageEvent
import com.ebicep.chatplus.features.chattabs.ChatTabAddNewMessageEvent
import com.ebicep.chatplus.features.chattabs.ChatTabGetMessageAtEvent
import com.ebicep.chatplus.hud.ChatRenderLineTextEvent
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.PlayerFaceRenderer
import net.minecraft.resources.ResourceLocation
import java.util.*

object PlayerHeadChatDisplay {

    private const val CACHE_EXPIRATION = 1000 * 60 * 10
    private const val HEAD_WIDTH_PADDED = PlayerFaceRenderer.SKIN_HEAD_WIDTH + 2
    private val NAME_REGEX = Regex("(§.)|\\W")
    private val playerNameUUIDs = mutableMapOf<String, TimedUUID>()
    private val playerHeads = mutableMapOf<UUID, ResourceLocation>()
    private var messageOffset = 0

    data class TimedUUID(val uuid: UUID, val lastUsed: Long)

    init {
        updateMessageOffset()
        EventBus.register<ChatPlusMinuteEvent> {
            if (it.minute % 10 == 0L) {
                val currentTime = System.currentTimeMillis()
                playerNameUUIDs.entries.removeIf { entry ->
                    currentTime - entry.value.lastUsed > CACHE_EXPIRATION
                }
            }
        }
        EventBus.register<ChatTabAddNewMessageEvent> {
            if (!Config.values.playerHeadChatDisplayEnabled) {
                return@register
            }
            val content = it.rawComponent.string
            val connection = Minecraft.getInstance().connection ?: return@register
            content.split(NAME_REGEX).forEach { word ->
                if (word.isBlank()) {
                    return@forEach
                }
                playerNameUUIDs[word]?.let { timedUUID ->
                    it.chatPlusGuiMessage.senderUUID = timedUUID.uuid
                    return@register
                }
                val playerInfo = connection.getPlayerInfo(word)
                if (playerInfo != null) {
                    val uuid = playerInfo.profile.id
                    playerNameUUIDs[word] = TimedUUID(uuid, System.currentTimeMillis())
                    playerHeads[uuid] = playerInfo.skin.texture
                    it.chatPlusGuiMessage.senderUUID = uuid
                    return@register
                }
            }
        }
        EventBus.register<ChatRenderLineTextEvent> {
            if (!Config.values.playerHeadChatDisplayEnabled) {
                return@register
            }
            val chatPlusGuiMessageLine = it.chatPlusGuiMessageLine
            val guiGraphics = it.guiGraphics
            val poseStack = guiGraphics.pose()
            if (!Config.values.playerHeadChatDisplayShowOnWrapped && chatPlusGuiMessageLine.wrappedIndex != 0) {
                if (Config.values.playerHeadChatDisplayOffsetNonHeadMessagesShowOnWrapped) {
                    poseStack.translate0(x = messageOffset)
                }
                return@register
            }
            val chatPlusGuiMessage = chatPlusGuiMessageLine.linkedMessage
            val senderUUID = chatPlusGuiMessage.senderUUID
            if (senderUUID == null) {
                if (Config.values.playerHeadChatDisplayOffsetNonHeadMessages) {
                    poseStack.translate0(x = messageOffset)
                }
                return@register
            }
            val resourceLocation = playerHeads[senderUUID]
            if (resourceLocation == null) {
                if (Config.values.playerHeadChatDisplayOffsetNonHeadMessages) {
                    poseStack.translate0(x = messageOffset)
                }
                return@register
            }
            poseStack.translate0(x = messageOffset)
            poseStack.createPose {
                poseStack.guiForward()
                poseStack.translate0(x = -HEAD_WIDTH_PADDED.toDouble())
                RenderSystem.enableBlend()
                RenderSystem.setShaderColor(1f, 1f, 1f, it.fadeOpacity.toFloat())
                PlayerFaceRenderer.draw(
                    guiGraphics,
                    resourceLocation,
                    it.chatWindow.renderer.rescaledX,
                    it.verticalTextOffset,
                    PlayerFaceRenderer.SKIN_HEAD_WIDTH
                )
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                RenderSystem.disableBlend()
            }
        }
        EventBus.register<ChatTabAddDisplayMessageEvent> {
            if (!Config.values.playerHeadChatDisplayEnabled) {
                return@register
            }
            it.maxWidth -= HEAD_WIDTH_PADDED
        }
        EventBus.register<ChatTabGetMessageAtEvent> {
            if (!Config.values.playerHeadChatDisplayEnabled) {
                return@register
            }
            val messageLine = it.chatTab.getMessageAtLineRelative(it.chatX, it.chatY) ?: return@register
            val senderUUID = messageLine.linkedMessage.senderUUID
            // -------
            // XXXXXXX
            if (senderUUID == null && !Config.values.playerHeadChatDisplayOffsetNonHeadMessages) {
                return@register
            }
            // XXXXXXX
            // _------
            if (messageLine.wrappedIndex != 0 && !Config.values.playerHeadChatDisplayOffsetNonHeadMessagesShowOnWrapped) {
                return@register
            }
            it.chatX -= messageOffset
        }
    }

    fun updateMessageOffset() {
        messageOffset = when (Config.values.messageAlignment) {
            AlignMessage.Alignment.LEFT -> HEAD_WIDTH_PADDED
            AlignMessage.Alignment.CENTER -> HEAD_WIDTH_PADDED / 2
            AlignMessage.Alignment.RIGHT -> 0
        }
    }

}