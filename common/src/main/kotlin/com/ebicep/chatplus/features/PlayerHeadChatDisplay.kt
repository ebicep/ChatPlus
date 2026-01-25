package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.ChatPlusMinuteEvent
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.AddNewMessageEvent
import com.ebicep.chatplus.features.chattabs.ChatTabAddDisplayMessageEvent
import com.ebicep.chatplus.features.chattabs.ChatTabGetMessageAtEvent
import com.ebicep.chatplus.features.chattabs.MessageAtType
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderLineTextEvent
import com.ebicep.chatplus.util.GraphicsUtil.PlayerHeadUtils.playerFaceRendererDraw
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.PlayerFaceRenderer
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.PlayerModelPart
import java.util.*

object PlayerHeadChatDisplay {

    private const val CACHE_EXPIRATION = 1000 * 60 * 10
    private const val HEAD_RIGHT_PADDING = 2
    private const val HEAD_WIDTH_PADDED = PlayerFaceRenderer.SKIN_HEAD_WIDTH + HEAD_RIGHT_PADDING
    private const val HEAD_WIDTH_PADDED_HALF = HEAD_WIDTH_PADDED / 2
    private val NAME_REGEX = Regex("(§.)|\\W")
    private val playerNameUUIDs = mutableMapOf<String, TimedUUID>()
    private val playerHeads = mutableMapOf<UUID, HeadData>()

    data class TimedUUID(val uuid: UUID, val lastUsed: Long)

    data class HeadData(val texture: () -> Identifier, val showHat: Boolean)

    init {
        EventBus.register<ChatPlusMinuteEvent> {
            if (it.minute % 10 == 0L) {
                val currentTime = System.currentTimeMillis()
                playerNameUUIDs.entries.removeIf { entry ->
                    val remove = currentTime - entry.value.lastUsed > CACHE_EXPIRATION
                    if (remove) {
                        playerHeads.remove(entry.value.uuid)
                    }
                    remove
                }
            }
        }
        EventBus.register<AddNewMessageEvent> {
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
                    it.senderUUID = timedUUID.uuid
                    return@register
                }
                val playerInfo: PlayerInfo? = connection.getPlayerInfo(word)
                if (playerInfo != null) {
                    val uuid = playerInfo.profile.id
                    playerNameUUIDs[word] = TimedUUID(uuid, System.currentTimeMillis())
                    playerHeads[uuid] = HeadData({ playerInfo.skin.body.texturePath() }, Minecraft.getInstance().level?.getPlayerByUUID(uuid)?.isModelPartShown(PlayerModelPart.HAT) == true)
                    it.senderUUID = uuid
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
            val messageOffset = getMessageOffset(it.chatWindow)
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
            val headData: HeadData? = playerHeads[senderUUID]
            if (headData == null) {
                if (Config.values.playerHeadChatDisplayOffsetNonHeadMessages) {
                    poseStack.translate0(x = messageOffset)
                }
                return@register
            }
            poseStack.translate0(x = messageOffset)
            poseStack.createPose {
                poseStack.translate0(x = -HEAD_WIDTH_PADDED.toDouble())
                it.textColor
                playerFaceRendererDraw(
                    guiGraphics,
                    headData.texture.invoke(),
                    it.chatWindow.renderer.rescaledX,
                    it.verticalTextOffset,
                    PlayerFaceRenderer.SKIN_HEAD_WIDTH.toFloat(),
                    headData.showHat,
                    false,
                    (it.textColor.coerceIn(0, 255) shl 24) or 0x00FFFFFF // alpha
                )
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
            if (it.messageAtType != MessageAtType.COMPONENT) {
                return@register
            }
            val messageLine = ChatManager.globalSelectedTab.getHoveredOverMessageLine() ?: return@register
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
            it.chatOperators.add { _, current ->
                current.x -= getMessageOffset(it.chatWindow)
            }
        }
    }

    private fun getMessageOffset(chatWindow: ChatWindow): Int {
        return when (chatWindow.generalSettings.messageAlignment) {
            AlignMessage.Alignment.LEFT -> HEAD_WIDTH_PADDED
            AlignMessage.Alignment.CENTER -> HEAD_WIDTH_PADDED_HALF
            AlignMessage.Alignment.RIGHT -> 0
        }
    }

}