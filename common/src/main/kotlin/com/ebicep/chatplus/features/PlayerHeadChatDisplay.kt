package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.ChatPlusMinuteEvent
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatTabAddDisplayMessageEvent
import com.ebicep.chatplus.features.chattabs.ChatTabAddNewMessageEvent
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderLineTextEvent
import com.ebicep.chatplus.hud.ChatRenderer
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.PlayerFaceRenderer
import net.minecraft.resources.ResourceLocation
import java.util.*

private const val CACHE_EXPIRATION = 1000 * 60 * 10
private const val HEAD_WIDTH_PADDED = PlayerFaceRenderer.SKIN_HEAD_WIDTH + 2

object PlayerHeadChatDisplay {

    private val NAME_REGEX = Regex("(§.)|\\W")
    private val playerNameUUIDs = mutableMapOf<String, TimedUUID>()
    private val playerHeads = mutableMapOf<UUID, ResourceLocation>()
    private var textOffset = 0

    data class TimedUUID(val uuid: UUID, val lastUsed: Long)

    init {
        updateTextOffset()
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
            val content = it.component.string
            val connection = Minecraft.getInstance().connection ?: return@register
            content.split(NAME_REGEX).forEach { word ->
                if (word.isBlank()) {
                    return@forEach
                }
                playerNameUUIDs[word]?.let { timedUUID ->
                    it.guiMessage.senderUUID = timedUUID.uuid
                    return@register
                }
                val playerInfo = connection.getPlayerInfo(word)
                if (playerInfo != null) {
                    val uuid = playerInfo.profile.id
                    playerNameUUIDs[word] = TimedUUID(uuid, System.currentTimeMillis())
                    playerHeads[uuid] = playerInfo.skin.texture
                    it.guiMessage.senderUUID = uuid
                    return@register
                }
            }
        }
        EventBus.register<ChatRenderLineTextEvent> {
            if (!Config.values.playerHeadChatDisplayEnabled) {
                return@register
            }
            val messages = ChatManager.selectedTab.messages
            val chatPlusGuiMessageLine = it.chatPlusGuiMessageLine
            val guiGraphics = it.guiGraphics
            val poseStack = guiGraphics.pose()
            if (!Config.values.playerHeadChatDisplayShowOnWrapped && chatPlusGuiMessageLine.wrappedIndex != 0) {
                if (Config.values.playerHeadChatDisplayOffsetNonHeadMessagesShowOnWrapped) {
                    poseStack.translate0(x = textOffset)
                }
                return@register
            }
            val chatPlusGuiMessage = messages[chatPlusGuiMessageLine.linkedMessageIndex]
            val senderUUID = chatPlusGuiMessage.senderUUID
            if (senderUUID == null) {
                if (Config.values.playerHeadChatDisplayOffsetNonHeadMessages) {
                    poseStack.translate0(x = textOffset)
                }
                return@register
            }
            val resourceLocation = playerHeads[senderUUID]
            if (resourceLocation == null) {
                if (Config.values.playerHeadChatDisplayOffsetNonHeadMessages) {
                    poseStack.translate0(x = textOffset)
                }
                return@register
            }
            poseStack.translate0(x = textOffset)
            poseStack.createPose {
                poseStack.guiForward()
                poseStack.translate0(x = -HEAD_WIDTH_PADDED.toDouble())
                PlayerFaceRenderer.draw(
                    guiGraphics,
                    resourceLocation,
                    ChatRenderer.rescaledX,
                    it.verticalTextOffset,
                    PlayerFaceRenderer.SKIN_HEAD_WIDTH
                )
            }
        }
        EventBus.register<ChatTabAddDisplayMessageEvent> {
            if (!Config.values.playerHeadChatDisplayEnabled) {
                return@register
            }
            it.maxWidth -= HEAD_WIDTH_PADDED
        }
    }

    fun updateTextOffset() {
        textOffset = when (Config.values.textAlignment) {
            AlignText.Alignment.LEFT -> HEAD_WIDTH_PADDED
            AlignText.Alignment.CENTER -> HEAD_WIDTH_PADDED / 2
            AlignText.Alignment.RIGHT -> 0
        }
    }

}