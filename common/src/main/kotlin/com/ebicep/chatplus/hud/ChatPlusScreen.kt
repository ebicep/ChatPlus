package com.ebicep.chatplus.hud

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.translator.LanguageManager
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.CommandSuggestions
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.util.Mth
import org.apache.commons.lang3.StringUtils

const val EDIT_BOX_HEIGHT = 14

data class ChatScreenKeyPressedEvent(
    val screen: ChatPlusScreen,
    val keyCode: Int,
    val scanCode: Int,
    val modifiers: Int,
    var returnFunction: Boolean = false
) : Event

data class ChatScreenMouseClickedEvent(
    val screen: ChatPlusScreen,
    val mouseX: Double,
    val mouseY: Double,
    val button: Int,
    var returnFunction: Boolean = false
) : Event

data class ChatScreenMouseScrolledEvent(
    val screen: ChatPlusScreen,
    val mouseX: Double,
    val mouseY: Double,
    val amount: Double,
    var returnFunction: Boolean = false
) : Event

data class ChatScreenMouseDraggedEvent(
    val screen: ChatPlusScreen,
    val mouseX: Double,
    val mouseY: Double,
    val button: Int,
    val dragX: Double,
    val dragY: Double,
) : Event

data class ChatScreenMouseReleasedEvent(
    val screen: ChatPlusScreen,
    val mouseX: Double,
    val mouseY: Double,
    val button: Int,
    var returnFunction: Boolean = false
) : Event

data class ChatScreenRenderEvent(
    val screen: ChatPlusScreen,
    val guiGraphics: GuiGraphics,
    val mouseX: Int,
    val mouseY: Int,
    val partialTick: Float,
) : Event

data class ChatScreenInputBoxEditEvent(
    val screen: ChatPlusScreen,
    val str: String,
    var returnFunction: Boolean = false
) : Event

data class ChatScreenInitPreEvent(
    val screen: ChatPlusScreen,
) : Event

data class ChatScreenInitPostEvent(
    val screen: ChatPlusScreen,
) : Event

data class ChatScreenCloseEvent(
    val screen: ChatPlusScreen,
) : Event

data class ChatScreenSendMessagePreEvent(
    val screen: ChatPlusScreen,
    var message: String,
    var returnFunction: Boolean = false
) : Event

data class ChatScreenSendMessagePostEvent(
    val screen: ChatPlusScreen,
    var message: String,
    var sentMessage: String,
    val messageToSend: String,
    val normalizeChatMessage: String,
    val messages: List<String>,
    var dontSendMessage: Boolean = false
) : Event


class ChatPlusScreen(pInitial: String) : Screen(Component.translatable("chat_plus_screen.title")) {

    private val USAGE_TEXT: Component = Component.translatable("chat_plus_screen.usage")
    private var historyBuffer = ""

    /**
     * keeps position of which chat message you will select when you press up, (does not increase for duplicated messages
     * sent immediately after each other)
     */
    private var historyPos = -1

    /** Chat entry field  */
    var input: EditBox? = null

    /** is the text that appears when you press the chat key and the input box appears pre-filled  */
    var initial: String = pInitial
    var editBoxWidth: Int = 0
    var commandSuggestions: CommandSuggestions? = null

    override fun init() {
        historyPos = ChatManager.sentMessages.size

        editBoxWidth = width

        EventBus.post(ChatScreenInitPreEvent(this))

        input = object : EditBox(
            minecraft!!.fontFilterFishy,
            2,
            height - EDIT_BOX_HEIGHT + 4,
            editBoxWidth + 1,
            EDIT_BOX_HEIGHT,
            Component.translatable("chatPlus.editBox")
        ) {
            override fun createNarrationMessage(): MutableComponent {
                return super.createNarrationMessage().append(commandSuggestions!!.narrationMessage)
            }
        }
        val editBox = input as EditBox
        initializeBaseEditBox(editBox)
        editBox.value = initial
        editBox.setResponder { str: String -> onEdited(str) }
        addWidget(editBox)
        setInitialFocus(editBox)
        editBox.isFocused = true
        commandSuggestions = CommandSuggestions(
            minecraft!!,
            this,
            editBox,
            font,
            false,
            false,
            1,
            Config.values.maxCommandSuggestions,
            true,
            -805306368
        )
        commandSuggestions!!.updateCommandInfo()

        EventBus.post(ChatScreenInitPostEvent(this))

    }

    fun <T> addWidget0(guiEventListener: T & Any): T where T : GuiEventListener?, T : NarratableEntry? {
        return addWidget(guiEventListener)
    }

    fun rebuildWidgets0() {
        rebuildWidgets()
    }

    fun initializeBaseEditBox(editBox: EditBox) {
        editBox.setMaxLength(256 * 5) // default 256
        editBox.setBordered(false)
        editBox.setCanLoseFocus(true)
    }

    override fun resize(pMinecraft: Minecraft, pWidth: Int, pHeight: Int) {
        val s = input!!.value
        this.init(pMinecraft, pWidth, pHeight)
        setChatLine(s)
        commandSuggestions!!.updateCommandInfo()
    }

    override fun removed() {
        EventBus.post(ChatScreenCloseEvent(this))
        ChatManager.selectedTab.resetChatScroll()
        ChatManager.selectedTab.refreshDisplayedMessage()
    }

    private fun onEdited(str: String) {
        if (EventBus.post(ChatScreenInputBoxEditEvent(this, str)).returnFunction) {
            return
        }
        val s = input!!.value
        commandSuggestions!!.setAllowSuggestions(s != initial)
        commandSuggestions!!.updateCommandInfo()
    }

    override fun keyPressed(pKeyCode: Int, pScanCode: Int, pModifiers: Int): Boolean {
        //input!!.setEditable(true)
        return when {
            commandSuggestions!!.keyPressed(pKeyCode, pScanCode, pModifiers) -> {
                true
            }

            EventBus.post(ChatScreenKeyPressedEvent(this, pKeyCode, pScanCode, pModifiers)).returnFunction -> {
                true
            }

            super.keyPressed(pKeyCode, pScanCode, pModifiers) -> {
                true
            }

            else -> when (pKeyCode) {
                256 -> { // escape
                    minecraft!!.setScreen(null as Screen?)
                    true
                }

                257, 335 -> { // enter
                    if (handleChatInput(input!!.value)) {
                        minecraft!!.setScreen(null as Screen?)
                    }
                    true
                }
                // cycle through own sent messages
                265 -> { // up arrow
                    moveInHistory(-1)
                    true
                }

                264 -> { // down arrow
                    moveInHistory(1)
                    true
                }
                // cycle through displayed chat messages
                266 -> { // page up
                    ChatManager.selectedTab.scrollChat(ChatManager.getLinesPerPage() - 1)
                    true
                }

                267 -> { // page down
                    ChatManager.selectedTab.scrollChat(-ChatManager.getLinesPerPage() + 1)
                    true
                }

                else -> {
                    false
                }
            }
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        var delta = Mth.clamp(amount, -1.0, 1.0)
        return if (commandSuggestions!!.mouseScrolled(delta)) {
            true
        } else {
            if (EventBus.post(ChatScreenMouseScrolledEvent(this, mouseX, mouseY, amount)).returnFunction) {
                return true
            }
            // control = no scroll
            // shift = fine scroll
            // alt = triple scroll
            val window = Minecraft.getInstance().window.window
            if (InputConstants.isKeyDown(window, Config.values.keyNoScroll.value)) {
                return true
            }
            if (InputConstants.isKeyDown(window, Config.values.keyLargeScroll.value)) {
                delta *= 21.0
            } else if (!InputConstants.isKeyDown(window, Config.values.keyFineScroll.value)) {
                delta *= 7.0
            }
            ChatManager.selectedTab.scrollChat(delta.toInt())
            true
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, pButton: Int): Boolean {
//        ChatPlus.LOGGER.info("Clicked at: $mouseX, $mouseY")
        return if (commandSuggestions!!.mouseClicked(mouseX.toInt().toDouble(), mouseY.toInt().toDouble(), pButton)) {
            true
        } else {
            if (EventBus.post(ChatScreenMouseClickedEvent(this, mouseX, mouseY, pButton)).returnFunction) {
                return true
            }
            if (pButton == 0) {
                if (ChatManager.selectedTab.handleChatQueueClicked(mouseX, mouseY)) {
                    return true
                }
                val style = ChatManager.selectedTab.getComponentStyleAt(mouseX, mouseY)
                if (style != null && handleComponentClicked(style)) {
                    initial = input!!.value
                    return true
                }
            }
            if (input!!.isFocused && input!!.mouseClicked(mouseX, mouseY, pButton)) {
                true
            } else {
                super.mouseClicked(mouseX, mouseY, pButton)
            }
        }
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (EventBus.post(ChatScreenMouseReleasedEvent(this, mouseX, mouseY, button)).returnFunction) {
            return true
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        EventBus.post(ChatScreenMouseDraggedEvent(this, mouseX, mouseY, button, dragX, dragY))
        if (!ChatManager.isChatFocused() || button != 0) { // forgot why this is here
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
        }
        return true
    }

    override fun insertText(pText: String, pOverwrite: Boolean) {
        if (pOverwrite) {
            input!!.value = pText
        } else {
            input!!.insertText(pText)
        }
    }

    /**
     * Input is relative and is applied directly to the sentHistoryCursor so -1 is the previous message, 1 is the next
     * message from the current cursor position.
     */
    fun moveInHistory(pMsgPos: Int) {
        var i = historyPos + pMsgPos
        val j = ChatManager.sentMessages.size
        i = Mth.clamp(i, 0, j)
        if (i != historyPos) {
            if (i == j) {
                historyPos = j
                input!!.value = historyBuffer
            } else {
                if (historyPos == j) {
                    historyBuffer = input!!.value
                }
                input!!.value = ChatManager.sentMessages[i]
                commandSuggestions!!.setAllowSuggestions(false)
                historyPos = i
            }
            setInitialFocus(input!!)
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        lastMouseX = mouseX
        lastMouseY = mouseY

        renderInputBox(guiGraphics, mouseX, mouseY, partialTick)

        super.render(guiGraphics, mouseX, mouseY, partialTick)

        // brigadier
        commandSuggestions!!.render(guiGraphics, mouseX, mouseY)

        // hoverables
        val style = ChatManager.selectedTab.getComponentStyleAt(mouseX.toDouble(), mouseY.toDouble())
        if (style?.hoverEvent != null) {
            guiGraphics.renderComponentHoverEffect(font, style, mouseX, mouseY)
        }

        EventBus.post(ChatScreenRenderEvent(this, guiGraphics, mouseX, mouseY, partialTick))
    }

    private fun renderInputBox(
        guiGraphics: GuiGraphics,
        pMouseX: Int,
        pMouseY: Int,
        pPartialTick: Float
    ) {
        guiGraphics.fill(
            if (LanguageManager.languageSpeakEnabled) 65 else 0,
            height - EDIT_BOX_HEIGHT,
            editBoxWidth,
            height,
            minecraft!!.options.getBackgroundColor(Int.MIN_VALUE)
        )
        input!!.render(guiGraphics, pMouseX, pMouseY, pPartialTick)
    }

    override fun renderBackground(guiGraphics: GuiGraphics) {
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

    private fun setChatLine(pChatLine: String) {
        input!!.value = pChatLine
    }

    override fun updateNarrationState(pOutput: NarrationElementOutput) {
        pOutput.add(NarratedElementType.TITLE, getTitle())
        pOutput.add(NarratedElementType.USAGE, USAGE_TEXT)
        val s = input!!.value
        if (s.isNotEmpty()) {
            pOutput.nest().add(NarratedElementType.TITLE, Component.translatable("chat_plus_screen.message", s))
        }
    }

    fun handleChatInput(rawMessage: String): Boolean {
        val sendMessageEvent = EventBus.post(ChatScreenSendMessagePreEvent(this, rawMessage))
        if (sendMessageEvent.returnFunction) {
            return minecraft!!.screen === this
        }
        val newMessage = sendMessageEvent.message
        val normalizeChatMessage = normalizeChatMessage(newMessage)
        if (normalizeChatMessage.isEmpty()) {
            return true
        }
        val messages = splitChatMessage(normalizeChatMessage)
        if (messages.isEmpty()) {
            return minecraft!!.screen === this
        }
        var sentMessage = messages[0]
        if (rawMessage != newMessage) {
            sentMessage = splitChatMessage(rawMessage)[0]
        }
        val messageToSend = messages[0]

        if (EventBus.post(
                ChatScreenSendMessagePostEvent(
                    this,
                    newMessage,
                    sentMessage,
                    messageToSend,
                    normalizeChatMessage,
                    messages
                )
            ).dontSendMessage
        ) {
            return minecraft!!.screen === this
        }

        ChatManager.addSentMessage(sentMessage)
        if (normalizeChatMessage.startsWith("/")) {
            minecraft!!.player!!.connection.sendCommand(messageToSend.substring(1))
        } else {
            minecraft!!.player!!.connection.sendChat(messageToSend)
            messagesToSend.addAll(messages.subList(1, messages.size))
        }
        return minecraft!!.screen === this
    }

    fun normalizeChatMessage(message: String): String {
        return StringUtils.normalizeSpace(message.trim { it <= ' ' })
    }

    fun font(): Font {
        return font
    }

    companion object {

        var lastMouseX = 0
        var lastMouseY = 0

        val messagesToSend: MutableList<String> = mutableListOf()
        var lastMessageSentTick = 0L

        fun splitChatMessage(message: String): List<String> {
            return if (message.length <= 256) {
                listOf(message)
            } else {
                val list = ArrayList<String>()
                var i = 0
                while (i < message.length) {
                    var j = i + 256
                    if (j >= message.length) {
                        j = message.length
                    }
                    list.add(message.substring(i, j))
                    i = j
                }
                list
            }
        }
    }


}