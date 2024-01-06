package com.ebicep.chatplus.hud

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.translator.SelfTranslator
import com.ebicep.chatplus.translator.languageSpeakEnabled
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.GuiMessage
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.CommandSuggestions
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.util.Mth
import org.apache.commons.lang3.StringUtils

const val EDIT_BOX_HEIGHT = 14
const val PADDING = 6
const val SPACER = 2 // space between text box / find / translate

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


class ChatPlusScreen(pInitial: String) : Screen(Component.translatable("chat_plus_screen.title")) {

    private val USAGE_TEXT: Component = Component.translatable("chat_plus_screen.usage")
    private var historyBuffer = ""

    /**
     * keeps position of which chat message you will select when you press up, (does not increase for duplicated messages
     * sent immediately after each other)
     */
    private var historyPos = -1

    /** Chat entry field  */
    var inputTranslatePrefix: EditBox? = null
    var input: EditBox? = null

    /** is the text that appears when you press the chat key and the input box appears pre-filled  */
    var initial: String = pInitial
    private var editBoxWidth: Int = 0
    private var textBarElements = mutableListOf<TextBarElement>()
    private var textBarElementsStartX: MutableMap<TextBarElement, Int> = mutableMapOf()
    var commandSuggestions: CommandSuggestions? = null

    override fun init() {
        historyPos = ChatManager.sentMessages.size

        editBoxWidth = width
        if (textBarElements.isEmpty()) {
            textBarElements.add(FindTextBarElement(this))
            if (Config.values.translatorEnabled) {
                textBarElements.add(TranslateSpeakTextBarElement(this))
            }

        }
        //____TEXTBOX_____-FIND--TRANSLATE-
        textBarElements.forEach {
            val calculatedWidth = it.getPaddedWidth() + SPACER
            editBoxWidth -= calculatedWidth
        }
        cacheTextBarElementXs()

        input = object : EditBox(
            minecraft!!.fontFilterFishy,
            if (languageSpeakEnabled) 68 else 2,
            height - EDIT_BOX_HEIGHT + 4,
            editBoxWidth + 1,
            EDIT_BOX_HEIGHT,
            Component.translatable("chatPlus.editBox")
        ) {
            override fun createNarrationMessage(): MutableComponent {
                return super.createNarrationMessage().append(commandSuggestions!!.narrationMessage)
            }
        }
        var editBox = input as EditBox
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
        commandSuggestions!!.setAllowHiding(false)
        commandSuggestions!!.updateCommandInfo()

        inputTranslatePrefix = null
        if (languageSpeakEnabled) {
            inputTranslatePrefix = EditBox(
                minecraft!!.fontFilterFishy,
                3,
                height - EDIT_BOX_HEIGHT + 4,
                63,
                EDIT_BOX_HEIGHT,
                Component.translatable("chatPlus.editBox")
            )
            editBox = inputTranslatePrefix as EditBox
            initializeBaseEditBox(editBox)
            addWidget(editBox)
        }
    }

    fun rebuildWidgets0() {
        rebuildWidgets()
    }

    private fun cacheTextBarElementXs() {
        var currentX = editBoxWidth + SPACER
        textBarElements.forEach {
            textBarElementsStartX[it] = currentX
            currentX += it.getPaddedWidth() + SPACER
        }
    }

    private fun initializeBaseEditBox(editBox: EditBox) {
        editBox.setMaxLength(256 * 5) // default 256
        editBox.isBordered = false
        editBox.setCanLoseFocus(true)
    }

    override fun resize(pMinecraft: Minecraft, pWidth: Int, pHeight: Int) {
        val s = input!!.value
        this.init(pMinecraft, pWidth, pHeight)
        setChatLine(s)
        commandSuggestions!!.updateCommandInfo()
    }

    override fun removed() {
        hoveredOverMessage = null
        ChatManager.selectedTab.resetChatScroll()
        findEnabled = false
        ChatManager.selectedTab.refreshDisplayedMessage()
    }

    private fun onEdited(str: String) {
        if (findEnabled) {
            ChatManager.selectedTab.refreshDisplayedMessage(str)
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

    override fun mouseScrolled(pMouseX: Double, pMouseY: Double, f: Double, pDelta: Double): Boolean {
        var delta = Mth.clamp(pDelta, -1.0, 1.0)
        return if (commandSuggestions!!.mouseScrolled(delta)) {
            true
        } else {
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
        return if (commandSuggestions!!.mouseClicked(mouseX.toInt().toDouble(), mouseY.toInt().toDouble(), pButton)) {
            true
        } else {
            EventBus.post(ChatScreenMouseClickedEvent(this, mouseX, mouseY, pButton))
            if (pButton == 0) {
                ChatManager.handleClickedTab(mouseX, mouseY)
                textBarElements.forEach {
                    val x = textBarElementsStartX[it]!!
                    if (x < mouseX && mouseX < x + it.getPaddedWidth() && height - EDIT_BOX_HEIGHT < mouseY && mouseY < height) {
                        it.onClick()
                    }
                }
                if (ChatManager.selectedTab.handleChatQueueClicked(mouseX, mouseY)) {
                    return true
                }
                val style = getComponentStyleAt(mouseX, mouseY)
                if (style != null && handleComponentClicked(style)) {
                    initial = input!!.value
                    return true
                }
            }
            if (input!!.isFocused && input!!.mouseClicked(mouseX, mouseY, pButton)) {
                true
            } else if (
                inputTranslatePrefix != null &&
                inputTranslatePrefix!!.isFocused &&
                inputTranslatePrefix!!.mouseClicked(mouseX, mouseY, pButton)
            ) {
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
        if (inputTranslatePrefix != null) {
            renderTranslateSpeakPrefixInputBox(guiGraphics, mouseX, mouseY, partialTick)
        }
        val currentY = height - EDIT_BOX_HEIGHT
        textBarElements.forEach {
            val elementStartX = textBarElementsStartX[it]!!
            it.onRender(guiGraphics, elementStartX, currentY, mouseX, mouseY, partialTick)
            if (elementStartX < mouseX && mouseX < elementStartX + it.getPaddedWidth() && height - EDIT_BOX_HEIGHT < mouseY && mouseY < height) {
                it.onHover(guiGraphics, mouseX, mouseY)
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick)

        // brigadier
        commandSuggestions!!.render(guiGraphics, mouseX, mouseY)

        // hoverables
        val style = getComponentStyleAt(mouseX.toDouble(), mouseY.toDouble())
        if (style?.hoverEvent != null) {
            guiGraphics.renderComponentHoverEffect(font, style, mouseX, mouseY)
        }

//        if (Config.values.hoverHighlightEnabled) {
//            hoveredOverMessage = if (movingChat) {
//                null
//            } else {
//                ChatManager.selectedTab.getMessageAt(lastMouseX.toDouble(), lastMouseY.toDouble())?.line
//            }
//        }
    }

    private fun renderTranslateSpeakPrefixInputBox(
        guiGraphics: GuiGraphics,
        pMouseX: Int,
        pMouseY: Int,
        pPartialTick: Float
    ) {
        guiGraphics.fill(
            0,
            height - EDIT_BOX_HEIGHT,
            65,
            height,
            minecraft!!.options.getBackgroundColor(Int.MIN_VALUE)
        )
        guiGraphics.renderOutline(
            0,
            height - EDIT_BOX_HEIGHT,
            65,
            EDIT_BOX_HEIGHT - 1,
            0xFF55FF55.toInt()
        )
        inputTranslatePrefix!!.render(guiGraphics, pMouseX, pMouseY, pPartialTick)
        if (
            pMouseX in 0 until 65 &&
            pMouseY in height - EDIT_BOX_HEIGHT until height
        ) {
            guiGraphics.renderTooltip(font, Component.translatable("chatPlus.translator.translateSpeakPrefix.tooltip"), pMouseX, pMouseY)
        }
    }

    private fun renderInputBox(
        guiGraphics: GuiGraphics,
        pMouseX: Int,
        pMouseY: Int,
        pPartialTick: Float
    ) {
        guiGraphics.fill(
            if (languageSpeakEnabled) 65 else 0,
            height - EDIT_BOX_HEIGHT,
            editBoxWidth,
            height,
            minecraft!!.options.getBackgroundColor(Int.MIN_VALUE)
        )
        input!!.render(guiGraphics, pMouseX, pMouseY, pPartialTick)
    }

    override fun renderBackground(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
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

    private fun getComponentStyleAt(pMouseX: Double, pMouseY: Double): Style? {
        return ChatManager.selectedTab.getClickedComponentStyleAt(pMouseX, pMouseY)
    }

    fun handleChatInput(pInput: String): Boolean {
        val normalizeChatMessage = normalizeChatMessage(pInput)
        return if (normalizeChatMessage.isEmpty()) {
            true
        } else {
            if (normalizeChatMessage.startsWith("/")) {
                val command = splitChatMessage(normalizeChatMessage)[0]
                ChatManager.addSentMessage(command)
                minecraft!!.player!!.connection.sendCommand(command.substring(1))
            } else {
                if (languageSpeakEnabled) {
                    SelfTranslator(normalizeChatMessage, if (inputTranslatePrefix == null) "" else inputTranslatePrefix!!.value).start()
                } else {
                    val messages = splitChatMessage(normalizeChatMessage)
                    if (messages.isEmpty()) {
                        return minecraft!!.screen === this
                    }
                    ChatManager.addSentMessage(messages[0])
                    minecraft!!.player!!.connection.sendChat(messages[0])
                    messagesToSend.addAll(messages.subList(1, messages.size))

                }
            }
            minecraft!!.screen === this // FORGE: Prevent closing the screen if another screen has been opened.
        }
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

        var hoveredOverMessage: GuiMessage.Line? = null

        var lastCopiedMessage: Pair<GuiMessage.Line, Long>? = null
        var copiedMessageCooldown = -1L

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