package com.ebicep.chatplus.features.chatwindows

import com.ebicep.chatplus.config.AnchorPoint
import com.ebicep.chatplus.config.MessageDirection
import com.ebicep.chatplus.features.AlignMessage
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.util.KotlinUtil.reduceAlpha
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.util.Mth
import java.awt.Color

@Serializable
class GeneralSettings {

    var disabled: Boolean = false
    var backgroundColor: Int = Color(0f, 0f, 0f, .5f).rgb
    var unfocusedBackgroundColorOpacityMultiplier: Float = .4f
    var reduceUnfocusedBackgroundOpacityWhenClosed: Boolean = false
    var scale: Float = 1f
    var textOpacity: Float = 1f
    var textShadow: Boolean = true
    var unfocusedTextOpacityMultiplier: Float = 1f
    var reduceUnfocusedTextOpacityWhenClosed: Boolean = false
    var unfocusedHeight: Float = .5f
    var lineSpacing: Float = 0f
    var anchorPoint: AnchorPoint = AnchorPoint.BOTTOM_LEFT
    var messageAlignment: AlignMessage.Alignment = AlignMessage.Alignment.LEFT
    var messageDirection: MessageDirection = MessageDirection.BOTTOM_UP
    var topDownDirectionWrapInOrder: Boolean = true
    var resetScrollPositionOnClose: Boolean = true

    @Transient
    lateinit var chatWindow: ChatWindow

    init {
        // correct values
        scale = Mth.clamp(scale, 0f, 1f)
        textOpacity = Mth.clamp(textOpacity, 0f, 1f)
        unfocusedHeight = Mth.clamp(unfocusedHeight, 0f, 1f)
        lineSpacing = Mth.clamp(lineSpacing, 0f, 1f)
    }

    fun clone(): GeneralSettings {
        return GeneralSettings().also {
            it.backgroundColor = backgroundColor
            it.unfocusedBackgroundColorOpacityMultiplier = unfocusedBackgroundColorOpacityMultiplier
            it.reduceUnfocusedBackgroundOpacityWhenClosed = reduceUnfocusedBackgroundOpacityWhenClosed
            it.anchorPoint = anchorPoint
            it.scale = scale
            it.textOpacity = textOpacity
            it.unfocusedTextOpacityMultiplier = unfocusedTextOpacityMultiplier
            it.reduceUnfocusedTextOpacityWhenClosed = reduceUnfocusedTextOpacityWhenClosed
            it.unfocusedHeight = unfocusedHeight
            it.lineSpacing = lineSpacing
            it.messageAlignment = messageAlignment
            it.messageDirection = messageDirection
        }
    }

    fun getUpdatedBackgroundColor(): Int {
        val isFocused = chatWindow == ChatManager.selectedWindow
        val applyReductionWhenClosed = reduceUnfocusedBackgroundOpacityWhenClosed && !ChatManager.isChatFocused()
        if (isFocused && !applyReductionWhenClosed) {
            return backgroundColor
        }
        return reduceAlpha(backgroundColor, unfocusedBackgroundColorOpacityMultiplier)
    }

    fun getUpdatedTextOpacity(): Float {
        val isFocused = chatWindow == ChatManager.selectedWindow
        val applyReductionWhenClosed = reduceUnfocusedTextOpacityWhenClosed && !ChatManager.isChatFocused()
        if (isFocused && !applyReductionWhenClosed) {
            return textOpacity
        }
        return textOpacity * unfocusedTextOpacityMultiplier
    }

}