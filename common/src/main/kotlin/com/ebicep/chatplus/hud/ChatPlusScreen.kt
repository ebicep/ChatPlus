package com.ebicep.chatplus.hud

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.Events
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.GuiMessage
import net.minecraft.client.Minecraft
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
import kotlin.math.roundToInt

class ChatPlusScreen(pInitial: String) : Screen(Component.translatable("chat_plus_screen.title")) {

    val MOUSE_SCROLL_SPEED = 7.0
    private val USAGE_TEXT: Component = Component.translatable("chat_plus_screen.usage")
    private val TOOLTIP_MAX_WIDTH = 210
    private var historyBuffer = ""

    /**
     * keeps position of which chat message you will select when you press up, (does not increase for duplicated messages
     * sent immediately after each other)
     */
    private var historyPos = -1

    /** Chat entry field  */
    private var input: EditBox? = null

    /** is the text that appears when you press the chat key and the input box appears pre-filled  */
    private var initial: String = pInitial
    var commandSuggestions: CommandSuggestions? = null

    override fun init() {
        historyPos = ChatManager.sentMessages.size
        input = object : EditBox(minecraft!!.fontFilterFishy, 4, height - 12, width - 4, 12, Component.translatable("chatPlus.editBox")) {
            override fun createNarrationMessage(): MutableComponent {
                return super.createNarrationMessage().append(commandSuggestions!!.narrationMessage)
            }
        }
        val editBox = input as EditBox
        editBox.setMaxLength(256 * 5) // default 256
        editBox.setBordered(false)
        editBox.value = initial
        editBox.setResponder { str: String -> onEdited(str) }
        editBox.setCanLoseFocus(false)
        addWidget(editBox)
        commandSuggestions = CommandSuggestions(minecraft!!, this, editBox, font, false, false, 1, 10, true, -805306368)
        commandSuggestions!!.updateCommandInfo()
        setInitialFocus(editBox)
    }

    override fun resize(pMinecraft: Minecraft, pWidth: Int, pHeight: Int) {
        val s = input!!.value
        this.init(pMinecraft, pWidth, pHeight)
        setChatLine(s)
        commandSuggestions!!.updateCommandInfo()
    }

    override fun removed() {
        ChatManager.selectedTab.resetChatScroll()
    }

    override fun tick() {
        input!!.tick()
    }

    private fun onEdited(str: String) {
        val s = input!!.value
        commandSuggestions!!.setAllowSuggestions(s != initial)
        commandSuggestions!!.updateCommandInfo()
    }

    override fun keyPressed(pKeyCode: Int, pScanCode: Int, pModifiers: Int): Boolean {
        val window = Minecraft.getInstance().window.window
        val copyMessage = Config.values.keyCopyMessage
        val copyMessageModifier = Config.values.keyCopyMessageModifier
        return if (commandSuggestions!!.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            true
        } else if (
            copiedMessageCooldown < Events.currentTick &&
            InputConstants.isKeyDown(window, copyMessage.value) &&
            InputConstants.isKeyDown(window, copyMessageModifier.value)
        ) {
            copiedMessageCooldown = Events.currentTick + 20
            ChatManager.selectedTab.getMessageAt(lastMouseX.toDouble(), lastMouseY.toDouble())?.let {
                copyToClipboard(it.content)
                lastCopiedMessage = Pair(it.line, Events.currentTick + 60)
            }
            true
        } else if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            true
        } else if (pKeyCode == 256) { // escape
            minecraft!!.setScreen(null as Screen?)
            true
        } else if (pKeyCode == 257 || pKeyCode == 335) {
            if (handleChatInput(input!!.value, true)) {
                minecraft!!.setScreen(null as Screen?)
            }
            true
        } else { // enter
            when (pKeyCode) {
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

    private fun copyToClipboard(str: String) {
        Minecraft.getInstance().keyboardHandler.clipboard = str
    }

    override fun mouseScrolled(pMouseX: Double, pMouseY: Double, pDelta: Double): Boolean {
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

    override fun mouseClicked(pMouseX: Double, pMouseY: Double, pButton: Int): Boolean {
        return if (commandSuggestions!!.mouseClicked(pMouseX.toInt().toDouble(), pMouseY.toInt().toDouble(), pButton)) {
            true
        } else {
            if (pButton == 0) {
                val side = ChatManager.getX() + ChatManager.getWidth()
                val sideInner = ChatManager.getX() + ChatManager.getWidth() - renderingMovingSize
                val roof = ChatManager.getY() - ChatManager.getHeight()
                val roofInner = ChatManager.getY() - ChatManager.getHeight() + renderingMovingSize
                if (pMouseX > sideInner && pMouseX < side && pMouseY > roof && pMouseY < ChatManager.getY()) {
                    movingChatX = true
                }
                if (pMouseY < roofInner && pMouseY > roof && pMouseX > ChatManager.getX() && pMouseX < side) {
                    movingChatY = true
                }
                if (!movingChatX && !movingChatY) {
                    if (
                        pMouseX > ChatManager.getX() && pMouseX < sideInner &&
                        pMouseY > roofInner && pMouseY < ChatManager.getY()
                    ) {
                        movingChatBox = true
                        xDisplacement = pMouseX - ChatManager.getX()
                        yDisplacement = pMouseY - ChatManager.getY()
                    }
                }
                ChatManager.handleClickedCategory(pMouseX, pMouseY)
                if (ChatManager.selectedTab.handleChatQueueClicked(pMouseX, pMouseY)) {
                    return true
                }
                val style = getComponentStyleAt(pMouseX, pMouseY)
                if (style != null && handleComponentClicked(style)) {
                    initial = input!!.value
                    return true
                }
            }
            if (input!!.mouseClicked(pMouseX, pMouseY, pButton)) {
                true
            } else {
                super.mouseClicked(pMouseX, pMouseY, pButton)
            }
        }
    }

    override fun mouseReleased(pMouseX: Double, pMouseY: Double, pButton: Int): Boolean {
        if (movingChat) {
            movingChat = false
            return true
        }
        return super.mouseReleased(pMouseX, pMouseY, pButton)
    }

    override fun mouseDragged(pMouseX: Double, pMouseY: Double, pButton: Int, pDragX: Double, pDragY: Double): Boolean {
        val window = Minecraft.getInstance().window.window
        val moving = InputConstants.isKeyDown(window, Config.values.keyMoveChat.value)
        if (!ChatManager.isChatFocused() || !moving || pButton != 0) {
            movingChat = false
            return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)
        }
//        if (
//            pMouseX > Minecraft.getInstance().window.guiScaledWidth || pMouseY > Minecraft.getInstance().window.guiScaledHeight ||
//            pMouseX < 0 || pMouseY < 0
//        ) {
//            //movingChat = false
//            return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)
//        }
        if (movingChatX) {
            val newWidth: Double = Mth.clamp(
                pMouseX - ChatManager.getX(),
                ChatManager.getMinWidthScaled().toDouble(),
                Minecraft.getInstance().window.guiScaledWidth - ChatManager.getX() - 1.0
            )
            val width = newWidth.roundToInt()
            Config.values.chatWidth = width
        }
        if (movingChatY) {
            val newHeight: Double = Mth.clamp(
                ChatManager.getY() - pMouseY,
                ChatManager.getMinHeightScaled().toDouble(),
                ChatManager.getY() - 1.0
            )
            val height = newHeight.roundToInt()
            Config.values.chatHeight = height
        }
        if (movingChatBox) {
            Config.values.x = Mth.clamp(
                (pMouseX - xDisplacement).roundToInt(),
                0,
                Minecraft.getInstance().window.guiScaledWidth - ChatManager.getWidth() - 1
            )
            var newY = Mth.clamp(
                (pMouseY - yDisplacement).roundToInt(),
                ChatManager.getHeight() + 1,
                Minecraft.getInstance().window.guiScaledHeight - baseYOffset
            )
            if (newY == Minecraft.getInstance().window.guiScaledHeight - baseYOffset) {
                newY = -baseYOffset
            }
            Config.values.y = newY
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
        }
    }

    override fun render(guiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        lastMouseX = pMouseX
        lastMouseY = pMouseY
        // input box
        guiGraphics.fill(0, height - 14, width, height, minecraft!!.options.getBackgroundColor(Int.MIN_VALUE))
        guiGraphics.pose().pushPose()
        guiGraphics.pose().translate(-2.0, 2.0, 0.0)
        input!!.render(guiGraphics, pMouseX, pMouseY, pPartialTick)
        guiGraphics.pose().popPose()

        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick)

        // brigadier
        commandSuggestions!!.render(guiGraphics, pMouseX, pMouseY)

        // hoverables
        val guiMessageTag = ChatManager.selectedTab.getMessageTagAt(pMouseX.toDouble(), pMouseY.toDouble())
        if (guiMessageTag?.text() != null) {
            //guiGraphics.renderTooltip(font, font.split(guiMessageTag.text()!!, 210), pMouseX, pMouseY)
        } else {
            val style = getComponentStyleAt(pMouseX.toDouble(), pMouseY.toDouble())
            if (style?.hoverEvent != null) {
                guiGraphics.renderComponentHoverEffect(font, style, pMouseX, pMouseY)
            }
        }
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

    fun handleChatInput(pInput: String, pAddToRecentChat: Boolean): Boolean {
        val input = normalizeChatMessage(pInput)
        return if (input.isEmpty()) {
            true
        } else {
            if (input[0].startsWith("/")) {
                val command = input[0]
                if (pAddToRecentChat) {
                    ChatManager.addSentMessage(command)
                }
                minecraft!!.player!!.connection.sendCommand(command.substring(1))
            } else {
                input.forEach {
                    if (pAddToRecentChat) {
                        ChatManager.addSentMessage(it)
                    }
                    minecraft!!.player!!.connection.sendChat(it)
                }
            }
            minecraft!!.screen === this // FORGE: Prevent closing the screen if another screen has been opened.
        }
    }

    fun normalizeChatMessage(pMessage: String): List<String> {
        val normalizeSpace = StringUtils.normalizeSpace(pMessage.trim { it <= ' ' })

        return if (normalizeSpace.length <= 256) {
            listOf(normalizeSpace)
        } else {
            val list = ArrayList<String>()
            var i = 0
            while (i < normalizeSpace.length) {
                var j = i + 256
                if (j >= normalizeSpace.length) {
                    j = normalizeSpace.length
                }
                list.add(normalizeSpace.substring(i, j))
                i = j
            }
            list
        }
        //return StringUtil.trimChatMessage(normalizeSpace)
    }

    companion object {
        var movingChat: Boolean
            get() = movingChatX || movingChatY || movingChatBox
            set(value) {
                queueUpdateConfig = true
                movingChatX = value
                movingChatY = value
                movingChatBox = value
            }
        var movingChatX = false
        var movingChatY = false
        var movingChatBox = false
        var xDisplacement = 0.0
        var yDisplacement = 0.0

        var lastMouseX = 0
        var lastMouseY = 0

        var lastCopiedMessage: Pair<GuiMessage.Line, Long>? = null
        var copiedMessageCooldown = -1L
    }


}