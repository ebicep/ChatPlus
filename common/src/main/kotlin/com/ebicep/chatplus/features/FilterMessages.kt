package com.ebicep.chatplus.features

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.SoundWrapper
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatTabAddNewMessageEvent
import com.ebicep.chatplus.features.internal.MessageFilter
import com.ebicep.chatplus.hud.ChatRenderPreLineAppearanceEvent
import kotlinx.serialization.Serializable
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import java.awt.Color

object FilterMessages {

    val DEFAULT_COLOR = Color(0, 200, 0, 50).rgb

    init {
        EventBus.register<ChatTabAddNewMessageEvent> {
            if (!Config.values.filterMessagesEnabled) {
                return@register
            }
            val message = it.rawComponent!!.string
            for (filterHighlight in Config.values.filterMessagesPatterns) {
                if (!filterHighlight.playSound || !filterHighlight.matches(message)) {
                    continue
                }
                val sound = filterHighlight.sound
                try {
                    ResourceLocation.tryParse(sound.sound)?.let {
                        Minecraft.getInstance().soundManager.play(
                            SimpleSoundInstance(
                                it,
                                sound.source,
                                sound.volume,
                                sound.pitch,
                                RandomSource.create(),
                                false, // looping
                                0, // delay
                                SoundInstance.Attenuation.NONE,
                                0.0, // x
                                0.0, // y
                                0.0, // z
                                true // relative
                            )
                        )
                    }
                } catch (e: Exception) {
                    ChatPlus.LOGGER.error(e)
                }
            }
        }
        EventBus.register<ChatRenderPreLineAppearanceEvent>({ Config.values.filterMessagesLinePriority }) {
            if (!Config.values.filterMessagesEnabled) {
                return@register
            }
            val message = it.chatPlusGuiMessageLine.content
            for (filterHighlight in Config.values.filterMessagesPatterns) {
                if (!filterHighlight.changeColor || !filterHighlight.matches(message)) {
                    continue
                }
                it.backgroundColor = filterHighlight.color
            }
        }
    }

    @Serializable
    class Filter : MessageFilter {

        var changeColor: Boolean = true
        var color: Int = DEFAULT_COLOR
        var playSound: Boolean = false
        var sound: SoundWrapper = SoundWrapper()

        constructor(pattern: String, color: Int) : super(pattern) {
            this.color = color
        }

    }

}


//                if (filterHighlight.highlightTextOnly) {
//                    // calculate x position of the text
//                    val components = mutableListOf<MutableComponent>()
//                    val content = it.chatPlusGuiMessageLine.content
//                    val find = filterHighlight.regex.pattern.toRegex().find(ChatFormatting.stripFormatting(content)!!) ?: continue
//                    it.line.content.accept { index, style, code ->
//                        components.add(Component.literal(ChatFormatting.stripFormatting(content)!![index].toString()).withStyle(style))
//                        true
//                    }
//
////                    Minecraft.getInstance().font.width(it.line.content.
//                    val guiGraphics = it.guiGraphics
//                    val poseStack = guiGraphics.pose()
//                    val first = find.groups[1]?.range?.first!!
//                    val last = find.groups[1]?.range?.last!!
//                    var start = 0
//                    var end = 0
//                    for (i in 0 until last) {
//                        val component = components[i]
//                        val width = Minecraft.getInstance().font.width(component)
//                        if (i < first) {
//                            start += width
//                        }
//                        end += width
//                    }
//
//                    poseStack.createPose {
//                        poseStack.guiForward(amount = 75.0)
//                        //background
//                        guiGraphics.fill(
//                            rescaledX + start,
//                            it.verticalChatOffset - lineHeight,
//                            rescaledX + Minecraft.getInstance().font.width(it.line.content),
//                            it.verticalChatOffset,
//                            filterHighlight.color
//                        )
//                    }
//                }