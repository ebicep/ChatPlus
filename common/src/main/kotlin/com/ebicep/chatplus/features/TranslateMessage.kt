package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.textbarelements.FindToggleEvent
import com.ebicep.chatplus.features.textbarelements.TextBarElements
import com.ebicep.chatplus.features.textbarelements.TranslateSpeakTextBarElement
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.translator.SelfTranslator
import com.ebicep.chatplus.translator.Translator
import com.ebicep.chatplus.translator.languageTo
import dev.architectury.event.CompoundEventResult
import dev.architectury.event.events.client.ClientChatEvent
import dev.architectury.event.events.client.ClientSystemMessageEvent
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Component


object TranslateMessage {

    const val TRANSLATE_COLOR = 0xFFFFFF55
    var languageSpeakEnabled = false

    var inputTranslatePrefix: EditBox? = null

    init {
        EventBus.register<TextBarElements.AddTextBarElementEvent>(4) {
            if (Config.values.translatorEnabled) {
                it.elements.add(TranslateSpeakTextBarElement(it.screen))
            }
        }
        EventBus.register<ChatScreenInitPostEvent> {
            if (!Config.values.translatorEnabled) {
                return@register
            }
            val screen = it.screen

            inputTranslatePrefix = null
            if (languageSpeakEnabled) {
                screen.input?.x = 68
                inputTranslatePrefix = EditBox(
                    screen.minecraft!!.fontFilterFishy,
                    3,
                    screen.height - EDIT_BOX_HEIGHT + 4,
                    63,
                    EDIT_BOX_HEIGHT,
                    Component.translatable("chatPlus.editBox")
                )
                val editBox = inputTranslatePrefix as EditBox
                screen.initializeBaseEditBox(editBox)
                screen.addWidget0(editBox)
            }
        }
        EventBus.register<FindToggleEvent> {
            if (!Config.values.translatorEnabled) {
                return@register
            }
            if (languageSpeakEnabled) {
                languageSpeakEnabled = false
            }
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            if (!Config.values.translatorEnabled) {
                return@register
            }
            if (languageSpeakEnabled) {
                it.returnFunction = inputTranslatePrefix != null &&
                        inputTranslatePrefix!!.isFocused &&
                        inputTranslatePrefix!!.mouseClicked(it.mouseX, it.mouseY, it.button)
            }
        }
        EventBus.register<ChatScreenRenderEvent> {
            if (!Config.values.translatorEnabled) {
                return@register
            }
            if (inputTranslatePrefix == null) {
                return@register
            }
            val screen = it.screen
            val guiGraphics = it.guiGraphics
            val height = screen.height
            val minecraft = screen.minecraft!!
            guiGraphics.fill(
                0,
                height - EDIT_BOX_HEIGHT,
                65,
                height,
                minecraft.options.getBackgroundColor(Int.MIN_VALUE)
            )
            guiGraphics.renderOutline(
                0,
                height - EDIT_BOX_HEIGHT,
                65,
                EDIT_BOX_HEIGHT - 1,
                0xFF55FF55.toInt()
            )
            val mouseX = it.mouseX
            val mouseY = it.mouseY
            inputTranslatePrefix!!.render(guiGraphics, mouseX, mouseY, it.partialTick)
            if (
                mouseX in 0 until 65 &&
                mouseY in height - EDIT_BOX_HEIGHT until height
            ) {
                guiGraphics.renderTooltip(
                    minecraft.font,
                    Component.translatable("chatPlus.translator.translateSpeakPrefix.tooltip"),
                    mouseX,
                    mouseY
                )
            }
        }
        EventBus.register<ChatScreenSendMessagePostEvent> {
            if (!Config.values.translatorEnabled) {
                return@register
            }
            if (!languageSpeakEnabled) {
                return@register
            }
            it.dontSendMessage = true
            SelfTranslator(it.normalizeChatMessage, if (inputTranslatePrefix == null) "" else inputTranslatePrefix!!.value).start()
        }

        ClientChatEvent.RECEIVED.register { type: ChatType.Bound, component: Component ->
            handleTranslate(component)
            CompoundEventResult.pass()
        }
        ClientSystemMessageEvent.RECEIVED.register { component: Component ->
            handleTranslate(component)
            CompoundEventResult.pass()
        }
    }

    private fun handleTranslate(component: Component) {
        if (!Config.values.translatorEnabled) {
            return
        }
        val unformattedText = component.string
        languageTo?.let {
            Translator(unformattedText, null, it).start()
        }
    }


}