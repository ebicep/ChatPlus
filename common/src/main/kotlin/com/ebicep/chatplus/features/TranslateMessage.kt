package com.ebicep.chatplus.features

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.textbarelements.FindToggleEvent
import com.ebicep.chatplus.features.textbarelements.TextBarElements
import com.ebicep.chatplus.features.textbarelements.TranslateSpeakTextBarElement
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.hud.ChatPlusScreen.EDIT_BOX_HEIGHT
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.mixin.IMixinScreen
import com.ebicep.chatplus.translator.*
import com.ebicep.chatplus.util.ComponentUtil
import dev.architectury.event.CompoundEventResult
import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientChatEvent
import dev.architectury.event.events.client.ClientRawInputEvent
import dev.architectury.event.events.client.ClientSystemMessageEvent
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent


object TranslateMessage {

    const val TRANSLATE_COLOR = 0xFFFFFF55
    var languageSpeakEnabled = false

    var inputTranslatePrefix: EditBox? = null

    init {
        EventBus.register<TextBarElements.AddTextBarElementEvent>({ 0 }) {
            if (!Config.values.translatorEnabled) {
                return@register
            }
            if (!Config.values.translatorTextBarElementEnabled) {
                return@register
            }
            it.elements.add(TranslateSpeakTextBarElement(it.screen))
        }
        EventBus.register<ChatScreenInitPostEvent> {
            if (!Config.values.translatorEnabled) {
                return@register
            }
            val screen = it.screen

            inputTranslatePrefix = null
            if (languageSpeakEnabled) {
                screen as IMixinChatScreen
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
                editBox.setMaxLength(256 * 5) // default 256
                editBox.isBordered = false
                editBox.setCanLoseFocus(true)
                screen as IMixinScreen
                screen.callAddWidget(editBox)
            }
        }
        EventBus.register<ChatScreenCloseEvent> {
            if (!Config.values.translatorEnabled) {
                return@register
            }
            if (languageSpeakEnabled && !Config.values.translateKeepOnAfterChatClose) {
                languageSpeakEnabled = false
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
        ClientRawInputEvent.KEY_PRESSED.register { _, keyCode, _, _, modifiers ->
            if (ChatManager.isChatFocused()) {
                return@register EventResult.pass()
            }
            if (keyCode != Config.values.translateKey.key.value || modifiers != Config.values.translateKey.modifier.toInt()) {
                return@register EventResult.pass()
            }
            languageSpeakEnabled = true
            EventResult.interruptTrue()
        }
        EventBus.register<ChatScreenKeyPressedEvent> {
            if (Config.values.translateToggleKey.isDown()) {
                TranslateSpeakTextBarElement.toggleTranslateSpeak(it.screen)
            }
        }

        var translateClickCooldown = 0L
        EventBus.register<ChatScreenMouseClickedEvent>({ 100 }) {
            if (!Config.values.translatorEnabled || !Config.values.translateClickEnabled) {
                return@register
            }
            if (it.button != 0 || !Screen.hasControlDown()) {
                return@register
            }
            if (System.currentTimeMillis() - translateClickCooldown < 2_000) {
                return@register
            }
            ChatManager.selectedTab.getMessageLineAt(it.mouseX, it.mouseY)?.let { message ->
                translateClickCooldown = System.currentTimeMillis()
                // selected message compatibility, sends one translate request with all selected messages split by § then sends the
                // translated messages unsplit
                val selectedMessages = SelectChat.selectedMessages
                val messages: List<ChatTab.ChatPlusGuiMessage> = if (selectedMessages.contains(message)) {
                    SelectChat.getSelectedMessagesOrdered().map { it.linkedMessage }
                } else {
                    listOf(message.linkedMessage)
                }
                LanguageManager.languageTo?.let { to ->
                    it.returnFunction = true
                    ClickTranslator(
                        messages,
                        messages.joinToString("§") { ChatFormatting.stripFormatting(it.guiMessage.content.string)!! },
                        to
                    ).start()
                }
            }
        }
    }

    private fun handleTranslate(component: Component) {
        if (!Config.values.translatorEnabled) {
            return
        }
        LanguageManager.languageTo?.let {
            Translator(ChatFormatting.stripFormatting(component.string)!!, null, it).start()
        }
    }

    class ClickTranslator(val line: List<ChatTab.ChatPlusGuiMessage>, message: String, to: Language) :
        Translator(message, null, to, false) {

        override fun onTranslateSameMessage() {
            val component = Component.literal("")
            line.forEachIndexed { index, it ->
                component.append(it.guiMessage.content)
                if (index != line.size - 1) {
                    component.append(Component.literal("\n"))
                }
            }
            ChatPlus.sendMessage(
                ComponentUtil.translatable(
                    "chatPlus.translator.sameMessage",
                    ChatFormatting.RED,
                    HoverEvent(HoverEvent.Action.SHOW_TEXT, component)
                )
            )
        }

        override fun onTranslate(matchedRegex: String?, translatedMessage: TranslateResult, fromLanguage: String?) {
            translatedMessage.translatedText.split("§").forEachIndexed { index, it ->
                Minecraft.getInstance().player?.sendSystemMessage(
                    ComponentUtil.literal(
                        (matchedRegex ?: "") + it.trim() + " (" + (fromLanguage ?: "Unknown") + ")",
                        ChatFormatting.GREEN,
                        HoverEvent(HoverEvent.Action.SHOW_TEXT, line[index].guiMessage.content.copy())
                    )
                )
            }
        }

    }

}