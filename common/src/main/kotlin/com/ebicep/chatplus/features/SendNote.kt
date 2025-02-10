package com.ebicep.chatplus.features

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.EnumTranslatableName
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.TranslateMessage.languageSpeakEnabled
import com.ebicep.chatplus.features.chattabs.AddNewMessageEvent
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.textbarelements.AddTextBarElementEvent
import com.ebicep.chatplus.features.textbarelements.SendNoteEvent
import com.ebicep.chatplus.features.textbarelements.SendNoteTextBarElement
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatPlusScreen
import com.ebicep.chatplus.hud.ChatPlusScreen.splitChatMessage
import com.ebicep.chatplus.hud.ChatScreenMouseClickedEvent
import com.ebicep.chatplus.hud.ChatScreenSendMessagePostEvent
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.translator.LanguageManager
import com.ebicep.chatplus.translator.Translator
import com.ebicep.chatplus.util.KeyUtil.isDown
import kotlinx.serialization.Serializable
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.awt.Color
import java.util.*

object SendNote {

    val NOTE_COLOR = Color(255, 255, 0).rgb
    private var lastSentNoteTick = -1L
    private val notes: MutableSet<ChatTab.ChatPlusGuiMessage> = Collections.newSetFromMap(IdentityHashMap())

    init {
        EventBus.register<AddTextBarElementEvent>({ 200 }) {
            if (!Config.values.sendNoteEnabled) {
                return@register
            }
            if (Config.values.sendNoteTextBarElementEnabled) {
                it.elements.add(SendNoteTextBarElement(it.screen))
            }
        }
        EventBus.register<SendNoteEvent> {
            if (Config.values.translatorEnabled && languageSpeakEnabled) {
                NoteTranslator(it.message).start()
            } else {
                addNote(it.message)
            }
        }
        EventBus.register<ChatScreenSendMessagePostEvent>({ 5 }, { true }) {
            if (!Config.values.sendNoteEnabled) {
                return@register
            }
            if (!Config.values.sendNoteKey.isDown()) {
                return@register
            }
            EventBus.post(SendNoteEvent(it.messages.joinToString("")))
            (it.screen as IMixinChatScreen).input.value = ""
            it.dontSendMessage = true
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            if (!Config.values.sendNoteEnabled) {
                return@register
            }
            if (it.button != 0) {
                return@register
            }
            ChatManager.globalSelectedTab.getHoveredOverMessageLine(it.mouseX, it.mouseY)?.let { message ->
                if (!notes.contains(message.linkedMessage)) {
                    return@register
                }
                val minecraft = Minecraft.getInstance()
                val selectMode = if (Config.values.sendNoteSelectKey.isDown()) Config.values.sendNoteSelectModeKey else Config.values.sendNoteSelectMode
                val string = if (selectMode == NoteSelectMode.WHOLE_MESSAGE) message.linkedMessage.guiMessage.content.string else message.content
                val screen = it.screen as IMixinChatScreen
                when (Config.values.sendNoteClickMode) {
                    NoteClickMode.SEND -> {
                        val normalizeChatMessage = ChatPlusScreen.normalizeChatMessage(string)
                        if (normalizeChatMessage.isEmpty()) {
                            return@register
                        }
                        val messages = splitChatMessage(normalizeChatMessage)
                        if (messages.isEmpty()) {
                            return@register
                        }
                        var sentMessage = messages[0]
                        val messageToSend = it.screen.normalizeChatMessage(messages[0])
                        ChatManager.addSentMessage(sentMessage)
                        if (string.startsWith("/")) {
                            minecraft.player!!.connection.sendCommand(messageToSend.substring(1))
                        } else {
                            minecraft.player!!.connection.sendChat(messageToSend)
                            if (messages.size > 1) {
                                InputOverFlowAutoFill.addToQueue(messages.subList(1, messages.size))
                            }
                        }
                    }

                    NoteClickMode.COPY -> {
                        minecraft.keyboardHandler.clipboard = string
                    }

                    NoteClickMode.OVERRIDE_INPUT -> {
                        screen.input.value = string
                    }

                    NoteClickMode.APPEND_INPUT -> {
                        screen.input.insertText(string)
                    }

                }
            }

        }
    }

    private fun addNote(message: String) {
        message.replace("&", "§").split("\\n").forEach {
            val component = Component.literal(it)
            val newMessageResult = ChatManager.globalSelectedTab.addNewMessage(
                AddNewMessageEvent(
                    component.copy(),
                    component,
                    null,
                    null,
                    Minecraft.getInstance().gui.guiTicks,
                    null,
                )
            )
            newMessageResult.newDisplayMessageResult?.let {
                notes.add(newMessageResult.chatTabAddNewMessageEvent.chatPlusGuiMessage)
                resetScreenShotTick()
            }
        }
    }

    fun onCooldown(): Boolean {
        return lastSentNoteTick + 60 > Events.currentTick
    }

    private fun resetScreenShotTick() {
        lastSentNoteTick = Events.currentTick
    }

    class NoteTranslator(val toTranslate: String) : Thread() {

        override fun run() {
            LanguageManager.languageSpeak?.let {
                val translator = Translator(toTranslate, LanguageManager.languageSelf, it)
                val translateResult = translator.translate(toTranslate) ?: return
                addNote(translateResult.translatedText)
            }
        }

    }


    @Serializable
    enum class NoteClickMode(key: String) : EnumTranslatableName {
        SEND("chatPlus.sendNote.clickMode.send"),
        COPY("chatPlus.sendNote.clickMode.copy"),
        OVERRIDE_INPUT("chatPlus.sendNote.clickMode.overrideInput"),
        APPEND_INPUT("chatPlus.sendNote.clickMode.appendInput"),

        ;

        val translatable: Component = Component.translatable(key)

        override fun getTranslatableName(): Component {
            return translatable
        }

    }

    @Serializable
    enum class NoteSelectMode(key: String) : EnumTranslatableName {
        WHOLE_MESSAGE("chatPlus.sendNote.selectMode.wholeMessage"),
        LINE("chatPlus.sendNote.selectMode.line"),

        ;

        val translatable: Component = Component.translatable(key)

        override fun getTranslatableName(): Component {
            return translatable
        }

    }


}