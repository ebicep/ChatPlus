package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.MovableChat.InputBoxSettings.Companion.INPUT_BOX_PADDING
import com.ebicep.chatplus.features.MovableChat.InputBoxSettings.Companion.PADDED_INPUT_BOX_HEIGHT
import com.ebicep.chatplus.features.TranslateMessage.languageSpeakEnabled
import com.ebicep.chatplus.hud.ChatPlusScreen.EDIT_BOX_DISPLAY_HEIGHT
import com.ebicep.chatplus.hud.ChatPlusScreen.EDIT_BOX_HEIGHT
import com.ebicep.chatplus.hud.ChatScreenCloseEvent
import com.ebicep.chatplus.hud.ChatScreenMouseClickedEvent
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.mixin.IMixinScreen
import com.ebicep.chatplus.translator.Language
import com.ebicep.chatplus.translator.LanguageManager
import com.ebicep.chatplus.translator.LanguageManager.languages
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.network.chat.Component
import java.awt.Color

class TranslateSpeakTextBarElement(private val chatPlusScreen: ChatScreen) : TextBarElement {

    companion object {

        val TRANSLATE_COLOR = Color(0, 255, 0).rgb
        var selectorShow = false
        var selectorFilteredLanguages: List<Language> = mutableListOf()
        var selectorLastHoveredLanguage: Language? = null
        val selectorRenderBottom: Boolean
            get() = Config.values.vanillaInputBox || Minecraft.getInstance().window.guiScaledHeight - Config.values.inputBoxSettings.getCalculatedStartY() < Config.values.inputBoxSettings.getCalculatedStartY()
        val selectorStartY: Int
            get() = if (Config.values.vanillaInputBox) {
                Minecraft.getInstance().window.guiScaledHeight - 12 - EDIT_BOX_HEIGHT - 1
            } else if (selectorRenderBottom) {
                Config.values.inputBoxSettings.getCalculatedStartY() - EDIT_BOX_HEIGHT - INPUT_BOX_PADDING - 1
            } else {
                Config.values.inputBoxSettings.getCalculatedStartY() + PADDED_INPUT_BOX_HEIGHT + 1
            }

        init {
            EventBus.register<ChatScreenMouseClickedEvent> {
                if (!Config.values.translatorEnabled) {
                    return@register
                }
                if (!selectorShow) {
                    return@register
                }
            }
            EventBus.register<ChatScreenCloseEvent> {
                selectorShow = false
            }
        }

        fun toggleTranslateSpeak(chatPlusScreen: ChatScreen) {
            selectorShow = false
            languageSpeakEnabled = !languageSpeakEnabled
            EventBus.post(TranslateToggleEvent(languageSpeakEnabled))
            chatPlusScreen as IMixinScreen
            chatPlusScreen as IMixinChatScreen
            val lastInput = chatPlusScreen.input!!.value
            chatPlusScreen.callRebuildWidgets()
            chatPlusScreen.input.value = lastInput
        }
    }

    lateinit var selectorLanguageSearch: EditBox

    override fun init() {
        selectorFilteredLanguages = mutableListOf()
        selectorLanguageSearch = EditBox(
            chatPlusScreen.minecraft!!.fontFilterFishy,
            Minecraft.getInstance().window.guiScaledWidth - getPaddedWidth() - if (Config.values.vanillaInputBox) 2 else 0,
            selectorStartY,
            getPaddedWidth(),
            EDIT_BOX_DISPLAY_HEIGHT,
            Component.translatable("chatPlus.selectorLanguageSearch")
        )
        selectorLanguageSearch.setMaxLength(25)
        selectorLanguageSearch.isBordered = true
        selectorLanguageSearch.setCanLoseFocus(true)
        selectorLanguageSearch.setResponder { updateFilteredLanguages(it) }
        chatPlusScreen as IMixinScreen
        chatPlusScreen.callAddWidget(selectorLanguageSearch)
    }

    private fun updateFilteredLanguages(str: String) {
        val filtered = languages.filter {
            fun has(input: String?) = input?.contains(str, ignoreCase = true) == true
            has(it.name) || has(it.nameUnicode) || has(it.googleCode)
        }
        if (filtered.isEmpty()) {
            return
        }
        selectorFilteredLanguages = filtered
        if (selectorRenderBottom) {
            selectorFilteredLanguages = selectorFilteredLanguages.asReversed()
        }
    }

    override fun getWidth(): Int {
        return Minecraft.getInstance().font.width(Config.values.translateSpeak)
    }

    override fun getText(): String {
        return Config.values.translateSpeak
    }

    override fun onClick(button: Int) {
        if (button == 0) {
            toggleTranslateSpeak(chatPlusScreen)
        } else if (button == 1) {
            selectorShow = !selectorShow
            chatPlusScreen as IMixinScreen
            chatPlusScreen as IMixinChatScreen
            if (selectorShow) {
                updateFilteredLanguages("")
                chatPlusScreen.callSetInitialFocus(selectorLanguageSearch)
            } else {
                chatPlusScreen.callSetInitialFocus(chatPlusScreen.input)
            }
        }
    }

    override fun onClickEvent(event: ChatScreenMouseClickedEvent) {
        if (!selectorShow) {
            return
        }
        selectorLastHoveredLanguage?.let {
            Config.values.translateSpeak = it.name
            LanguageManager.updateTranslateLanguages()
            selectorFilteredLanguages = mutableListOf()
            selectorLanguageSearch.isFocused = false
            chatPlusScreen as IMixinScreen
            chatPlusScreen.callRebuildWidgets()
            return
        }
        event.returnFunction = selectorLanguageSearch.isFocused && selectorLanguageSearch.mouseClicked(event.mouseX, event.mouseY, event.button)
    }

    override fun onHover(guiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int) {
        chatPlusScreen as IMixinScreen
        guiGraphics.renderTooltip(
            chatPlusScreen.font,
            tooltip("chatPlus.translator.translateSpeak.chat.tooltip"),
            pMouseX,
            pMouseY
        )
    }

    override fun onRender(guiGraphics: GuiGraphics, currentX: Int, currentY: Int, mouseX: Int, mouseY: Int, partialTick: Float) {
        fill(guiGraphics, currentX, currentY)
        drawCenteredString(guiGraphics, currentX, currentY, if (languageSpeakEnabled) TRANSLATE_COLOR else -1)
        if (languageSpeakEnabled) {
            renderOutline(guiGraphics, currentX, currentY, TRANSLATE_COLOR)
        }
        if (!selectorShow) {
            return
        }
        selectorLanguageSearch.y = selectorStartY
        selectorLanguageSearch.render(guiGraphics, currentX, currentY, partialTick)
        val poseStack = guiGraphics.pose()
        val bottom = selectorRenderBottom
        var y = if (bottom) selectorStartY - 10 else selectorStartY + EDIT_BOX_HEIGHT
        poseStack.createPose {
            val guiScaledWidth = Minecraft.getInstance().window.guiScaledWidth
            var hovered = false
            selectorFilteredLanguages.forEach {
                val languageWidth = Minecraft.getInstance().font.width(it.name)
                val hovering = guiScaledWidth - languageWidth - 4 < mouseX && mouseX < guiScaledWidth && y < mouseY && mouseY < y + 10
                if (hovering) {
                    hovered = true
                    selectorLastHoveredLanguage = it
                }
                guiGraphics.fill(
                    guiScaledWidth - languageWidth - 4,
                    y,
                    guiScaledWidth,
                    y + 10,
                    0x55000000
                )
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    it.name,
                    guiScaledWidth - languageWidth - 2,
                    y + 1,
                    if (hovering) TRANSLATE_COLOR else -1
                )
                y += if (bottom) -10 else 10
            }
            if (!hovered) {
                selectorLastHoveredLanguage = null
            }
        }
    }

}

data class TranslateToggleEvent(val enabled: Boolean) : Event
