package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.TimestampMode
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.CompactMessages.literalIgnored
import com.ebicep.chatplus.features.internal.MessageFilter
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatPlusScreen
import com.ebicep.chatplus.hud.ChatRenderer
import com.google.common.base.Predicate
import com.google.common.collect.Lists
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.ChatFormatting
import net.minecraft.client.ComponentCollector
import net.minecraft.client.GuiMessage
import net.minecraft.client.GuiMessageTag
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.multiplayer.chat.ChatListener
import net.minecraft.locale.Language
import net.minecraft.network.chat.*
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.Mth
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.min

data class ChatTabAddNewMessageEvent(
    val chatTab: ChatTab,
    val guiMessage: ChatTab.ChatPlusGuiMessage,
    val componentWithTimeStamp: MutableComponent,
    val component: Component,
    val signature: MessageSignature?,
    val addedTime: Int,
    val tag: GuiMessageTag?,
    var returnFunction: Boolean = false
) : Event


data class ChatTabAddDisplayMessageEvent(
    val chatTab: ChatTab,
    val component: Component,
    val addedTime: Int,
    val tag: GuiMessageTag?,
    val linkedMessage: ChatTab.ChatPlusGuiMessage,
    var maxWidth: Int,
    var returnFunction: Boolean = false
) : Event

data class ChatTabRemoveMessageEvent(
    val chatTab: ChatTab,
    val guiMessage: ChatTab.ChatPlusGuiMessage,
    var returnFunction: Boolean = false
) : Event

data class ChatTabRemoveDisplayMessageEvent(
    val chatTab: ChatTab,
    val chatPlusGuiMessageLine: ChatTab.ChatPlusGuiMessageLine,
    var returnFunction: Boolean = false
) : Event

data class ChatTabGetMessageAtEvent(
    val chatTab: ChatTab,
    var chatX: Double,
    var chatY: Double,
) : Event

@Serializable
class ChatTab : MessageFilter {

    data class ChatPlusGuiMessage(
        val guiMessage: GuiMessage,
        var timesRepeated: Int = 1,
        var senderUUID: UUID? = null
    )

    data class ChatPlusGuiMessageLine(
        val line: GuiMessage.Line,
        val content: String,
        val linkedMessage: ChatPlusGuiMessage,
        val wrappedIndex: Int,
    )

    var name: String = ""
        set(value) {
            field = value
            width = -1
        }
    var autoPrefix: String = ""

    // priority of tab, when adding messages, tabs are sorted by priority first
    // if a message got added to a tab then any other tab with a lower priority will not get the message
    var priority: Int = 0

    // if true then priority will be ignored when deciding to "skip" this tab
    var alwaysAdd: Boolean = false

    // if true then tab loop will break if message is added to this tab, overrides alwaysAdds
    var skipOthers: Boolean = false

    constructor(name: String, pattern: String, autoPrefix: String = "", alwaysAdd: Boolean = false) : super(pattern) {
        this.name = name
        this.autoPrefix = autoPrefix
        this.alwaysAdd = alwaysAdd
    }

    @Transient
    val messages: MutableList<ChatPlusGuiMessage> = ArrayList()

    @Transient
    val displayedMessages: MutableList<ChatPlusGuiMessageLine> = ArrayList()

    @Transient
    var chatScrollbarPos: Int = 0

    @Transient
    var newMessageSinceScroll = false

    @Transient
    var resetDisplayMessageAtTick = -1L

    // cached values
    @Transient
    var width: Int = -1
        get() {
            if (field == -1) {
                field = PADDING + Minecraft.getInstance().font.width(this.name) + PADDING
                xEnd = xStart + field
            }
            return field
        }

    @Transient
    var xStart: Double = 0.0
        set(value) {
            field = value
            xEnd = field + width
        }

    @Transient
    var xEnd: Double = 0.0

    @Transient
    var y: Double = 0.0


    fun addNewMessage(
        component: Component,
        signature: MessageSignature?,
        addedTime: Int,
        tag: GuiMessageTag?
    ) {
        if (!matches(component.string)) {
            return
        }
        val componentWithTimeStamp: MutableComponent = getTimeStampedMessage(component)
        val chatPlusGuiMessage = ChatPlusGuiMessage(GuiMessage(addedTime, componentWithTimeStamp, signature, tag))
        if (EventBus.post(
                ChatTabAddNewMessageEvent(
                    this,
                    chatPlusGuiMessage,
                    componentWithTimeStamp,
                    component,
                    signature,
                    addedTime,
                    tag,
                )
            ).returnFunction
        ) {
            return
        }
        this.messages.add(chatPlusGuiMessage)
        while (this.messages.size > Config.values.maxMessages) {
            EventBus.post(ChatTabRemoveMessageEvent(this, this.messages.removeFirst()))
        }
        this.addNewDisplayMessage(componentWithTimeStamp, addedTime, tag, chatPlusGuiMessage)
    }

    private fun getTimeStampedMessage(component: Component): MutableComponent {
        val componentWithTimeStamp: MutableComponent = component.copy()
        if (Config.values.chatTimestampMode != TimestampMode.NONE) {
            addTimestampToComponent(componentWithTimeStamp, 0)
        }
        return componentWithTimeStamp
    }

    fun addNewDisplayMessage(
        component: MutableComponent,
        addedTime: Int,
        tag: GuiMessageTag?,
        linkedMessage: ChatPlusGuiMessage
    ) {
        val maxWidth = Mth.floor(ChatManager.getBackgroundWidth())
        val displayMessageEvent = ChatTabAddDisplayMessageEvent(this, component, addedTime, tag, linkedMessage, maxWidth)
        if (EventBus.post(displayMessageEvent).returnFunction) {
            return
        }
//        val timesRepeated = messages[linkedMessageIndex].timesRepeated
//        if (timesRepeated > 0) {
//            component.append(Component.literal(" ($timesRepeated)").withStyle { it.withColor(ChatFormatting.GRAY) })
//        }
        val list: List<Pair<FormattedCharSequence, String>> = wrapComponents(
            component,
            displayMessageEvent.maxWidth,
            Minecraft.getInstance().font
        )
        for (j in list.indices) {
            val chatPlusLine = list[j]
            val formattedCharSequence = chatPlusLine.first
            val content = chatPlusLine.second
            if (ChatManager.isChatFocused() && chatScrollbarPos > 0) {
                newMessageSinceScroll = true
                scrollChat(1)
            }
            val lastComponent = j == list.size - 1
            this.displayedMessages.add(
                ChatPlusGuiMessageLine(
                    GuiMessage.Line(addedTime, formattedCharSequence, tag, lastComponent),
                    content,
                    linkedMessage,
                    j
                )
            )
        }
        while (this.displayedMessages.isNotEmpty() && !this.messages.contains(this.displayedMessages[0].linkedMessage)) {
            EventBus.post(ChatTabRemoveDisplayMessageEvent(this, this.displayedMessages.removeFirst()))
        }
    }

    /**
     * Adds timestamp to bottom of chat message, works for most chat formats
     */
    private fun addTimestampToComponent(pChatComponent: MutableComponent, depth: Int) {
        val previousHover = pChatComponent.style.hoverEvent
        if (previousHover != null) {
            when (previousHover.action) {
                HoverEvent.Action.SHOW_TEXT -> {
                    val component: MutableComponent = (previousHover.getValue(HoverEvent.Action.SHOW_TEXT) as MutableComponent?)!!
                    if (component.siblings.javaClass.getName().contains("Immutable")) {
                        component.siblings = ArrayList(component.siblings)
                    }
                    component.append(getTimestamp(true))
                }
            }
        } else if (depth < 3) {
            pChatComponent.withStyle {
                it.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, getTimestamp(false)))
            }
            pChatComponent.siblings.forEach {
                if (it is MutableComponent) {
                    addTimestampToComponent(it, depth + 1)
                }
            }
        }
    }

    private fun getTimestamp(newLine: Boolean): Component {
        return literalIgnored((if (newLine) "\n" else "") + "Sent at ")
            .withStyle {
                it.withColor(ChatFormatting.GRAY)
            }
            .append(Component.literal(getCurrentTime())
                .withStyle {
                    it.withColor(ChatFormatting.YELLOW)
                })
            .append(Component.literal("."))
    }

    private fun getCurrentTime(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(Config.values.chatTimestampMode.format))
    }

    fun getMessageTagAt(pMouseX: Double, pMouseY: Double): GuiMessageTag? {
        val chatX: Double = this.screenToChatX(pMouseX)
        val chatY: Double = this.screenToChatY(pMouseY)
        val i: Int = this.getMessageEndIndexAt(chatX, chatY)
        if (i >= 0 && i < this.displayedMessages.size) {
            val guiMessageLine: GuiMessage.Line = this.displayedMessages[i].line
            val guiMessageTag = guiMessageLine.tag()
            if (guiMessageTag != null && this.hasSelectedMessageTag(chatX, guiMessageLine, guiMessageTag)) {
                return guiMessageTag
            }
        }
        return null
    }

    fun clear() {
        messages.clear()
        displayedMessages.clear()
    }

    fun getHoveredOverMessageLine(): ChatPlusGuiMessageLine? {
        return getMessageLineAt(ChatPlusScreen.lastMouseX.toDouble(), ChatPlusScreen.lastMouseY.toDouble())
    }

    fun getMessageLineAt(pMouseX: Double, pMouseY: Double): ChatPlusGuiMessageLine? {
        val x = screenToChatX(pMouseX)
        val y = screenToChatY(pMouseY)
        return getMessageAtLineRelative(x, y)
    }

    fun getMessageAtLineRelative(x: Double, y: Double): ChatPlusGuiMessageLine? {
        val i = getMessageLineIndexAt(x, y)
        val size = this.displayedMessages.size
        return if (i in 0 until size) {
            return this.displayedMessages[size - i - 1]
        } else {
            null
        }
    }

    private fun screenToChatX(pX: Double): Double {
        return (pX - ChatRenderer.x) / ChatRenderer.scale
    }

    private fun screenToChatY(pY: Double): Double {
        val yDiff: Double = ChatRenderer.y - pY
        return yDiff / (ChatRenderer.scale * ChatRenderer.lineHeight.toDouble())
    }

    private fun getMessageEndIndexAt(pMouseX: Double, pMouseY: Double): Int {
        var i: Int = this.getMessageLineIndexAt(pMouseX, pMouseY)
        return if (i == -1) {
            -1
        } else {
            while (i >= 0) {
                if (this.displayedMessages[i].line.endOfEntry()) {
                    return i
                }
                --i
            }
            i
        }
    }

    private fun getMessageLineIndexAt(pMouseX: Double, pMouseY: Double): Int {
        if (!ChatManager.isChatFocused() || Minecraft.getInstance().options.hideGui) {
            return -1
        }
        if (!(0.0 <= pMouseX && pMouseX <= Mth.floor(ChatRenderer.backgroundWidthEndX.toDouble()))) {
            return -1
        }
        val i = min(ChatRenderer.rescaledLinesPerPage, this.displayedMessages.size)
        if (!(0.0 <= pMouseY && pMouseY < i.toDouble())) {
            return -1
        }
        val j = Mth.floor(pMouseY + chatScrollbarPos.toDouble())
        if (j < 0 || j >= this.displayedMessages.size) {
            return -1
        }
        return j
    }

    private fun hasSelectedMessageTag(chatX: Double, pLine: GuiMessage.Line, pTag: GuiMessageTag): Boolean {
        return if (chatX < 0.0) {
            true
        } else {
            val guiMessageTagIcon = pTag.icon()
            if (guiMessageTagIcon == null) {
                false
            } else {
                val i: Int = this.getTagIconLeft(pLine)
                val j = i + guiMessageTagIcon.width
                chatX >= i.toDouble() && chatX <= j.toDouble()
            }
        }
    }

    fun getTagIconLeft(pLine: GuiMessage.Line): Int {
        return Minecraft.getInstance().font.width(pLine.content()) + 4
    }

    fun handleChatQueueClicked(pMouseX: Double, pMouseY: Double): Boolean {
        return if (ChatManager.isChatFocused() && !Minecraft.getInstance().options.hideGui) {
            val chatListener: ChatListener = Minecraft.getInstance().chatListener
            if (chatListener.queueSize() == 0L) {
                false
            } else {
                val d0 = pMouseX - 2.0
                val d1: Double = ChatManager.getY() - pMouseY
                if (d0 <= Mth.floor(ChatManager.getWidth().toDouble() / ChatManager.getScale())
                        .toDouble() && d1 < 0.0 && d1 > Mth.floor(-9.0 * ChatManager.getScale())
                        .toDouble()
                ) {
                    chatListener.acceptNextDelayedMessage()
                    true
                } else {
                    false
                }
            }
        } else {
            false
        }
    }

    fun drawTagIcon(pGuiGraphics: GuiGraphics, pLeft: Int, pBottom: Int, pTagIcon: GuiMessageTag.Icon) {
        val i = pBottom - pTagIcon.height - 1
        pTagIcon.draw(pGuiGraphics, pLeft, i)
    }

    fun resetChatScroll() {
        chatScrollbarPos = 0
        this.newMessageSinceScroll = false
    }

    fun scrollChat(positionIncrease: Int) {
        setScrollPos(chatScrollbarPos + positionIncrease)
    }

    fun setScrollPos(newPosition: Int) {
        var pos = newPosition
        val maxScroll = displayedMessages.size - ChatManager.getLinesPerPageScaled()
        if (pos > maxScroll) {
            pos = maxScroll
        }
        if (pos <= 0) {
            pos = 0
            this.newMessageSinceScroll = false
        }
        chatScrollbarPos = pos
    }

    fun moveToMessage(chatPlusScreen: ChatPlusScreen, message: ChatPlusGuiMessageLine) {
        val linkedMessage = message.linkedMessage
        val lineOffset = ChatManager.getLinesPerPageScaled() / 2 + 1 // center the message
        ChatManager.selectedTab.refreshDisplayedMessage()
        chatPlusScreen.rebuildWidgets0()
        val displayIndex =
            ChatManager.selectedTab.displayedMessages.indexOfFirst { line -> line.linkedMessage === linkedMessage }
        val scrollTo = ChatManager.selectedTab.displayedMessages.size - displayIndex - lineOffset
        ChatManager.selectedTab.scrollChat(scrollTo)
    }

    fun rescaleChat() {
        resetChatScroll()
        queueRefreshDisplayedMessages()
        ChatRenderer.updateCachedDimension()
    }

    private fun queueRefreshDisplayedMessages() {
        resetDisplayMessageAtTick = Events.currentTick + 2
    }

    fun refreshDisplayedMessage() {
        refreshDisplayedMessage(null)
    }

    fun refreshDisplayedMessage(filter: Predicate<ChatPlusGuiMessage>?) {
        resetDisplayMessageAtTick = -1
        displayedMessages.clear()
        for (i in messages.indices) {
            val chatPlusGuiMessage: ChatPlusGuiMessage = messages[i]
            val guiMessage: GuiMessage = chatPlusGuiMessage.guiMessage
            if (filter != null && !filter.test(chatPlusGuiMessage)) {
                continue
            }
            // assume all messages are mutable from adding timestamp method call
            this.addNewDisplayMessage(
                guiMessage.content() as MutableComponent,
                guiMessage.addedTime(),
                guiMessage.tag(),
                chatPlusGuiMessage
            )
        }
    }

    fun getComponentStyleAt(pMouseX: Double, pMouseY: Double): Style? {
        val messageAtEvent = EventBus.post(
            ChatTabGetMessageAtEvent(
                this,
                screenToChatX(pMouseX),
                screenToChatY(pMouseY)
            )
        )
        val x = messageAtEvent.chatX
        val y = messageAtEvent.chatY
        val i = getMessageLineIndexAt(x, y)
        val size = this.displayedMessages.size
        return if (i in 0 until size) {
            val guiMessageLine: GuiMessage.Line = this.displayedMessages[size - i - 1].line
            Minecraft.getInstance().font.splitter.componentStyleAtWidth(guiMessageLine.content(), Mth.floor(x))
        } else {
            null
        }
    }

    companion object {

        const val PADDING = 2

        private fun stripColor(pText: String): String? {
            return if (Minecraft.getInstance().options.chatColors().get()) pText else ChatFormatting.stripFormatting(pText)
        }

        fun wrapComponents(component: FormattedText, maxWidth: Int, font: Font): List<Pair<FormattedCharSequence, String>> {
            val componentCollector = ComponentCollector()
            component.visit({ style: Style, string: String ->
                componentCollector.append(FormattedText.of(stripColor(string)!!, style))
                Optional.empty<Any?>()
            }, Style.EMPTY)
            val list: MutableList<Pair<FormattedCharSequence, String>> = Lists.newArrayList()
            font.splitter.splitLines(
                componentCollector.resultOrEmpty,
                maxWidth,
                Style.EMPTY
            ) { formattedText: FormattedText, isNewLine: Boolean ->
                val formattedCharSequence = Language.getInstance().getVisualOrder(formattedText)
                // note: removed new line indent
                list.add(Pair(formattedCharSequence, formattedText.string))
            }
            return if (list.isEmpty()) {
                mutableListOf(Pair(FormattedCharSequence.EMPTY, ""))
            } else {
                list
            }
        }
    }

}