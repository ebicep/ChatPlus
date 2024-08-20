package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.JumpToMessageMode
import com.ebicep.chatplus.config.MessageDirection
import com.ebicep.chatplus.config.TimestampMode
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.CompactMessages.literalIgnored
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.features.internal.MessageFilter
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatPlusScreen
import com.ebicep.chatplus.mixin.IMixinScreen
import com.ebicep.chatplus.util.KotlinUtil.containsReference
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
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.locale.Language
import net.minecraft.network.chat.*
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.Mth
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentHashMap.KeySetView
import kotlin.math.min


data class ChatTabAddNewMessageEvent(
    val chatWindow: ChatWindow,
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
    val chatWindow: ChatWindow,
    val chatTab: ChatTab,
    val component: Component,
    val addedTime: Int,
    val tag: GuiMessageTag?,
    val linkedMessage: ChatTab.ChatPlusGuiMessage,
    var maxWidth: Int,
    var addMessage: Boolean = true,
    var filtered: Boolean = false,
) : Event

data class ChatTabRemoveMessageEvent(
    val chatWindow: ChatWindow,
    val chatTab: ChatTab,
    val guiMessage: ChatTab.ChatPlusGuiMessage,
    var returnFunction: Boolean = false
) : Event

data class ChatTabRemoveDisplayMessageEvent(
    val chatWindow: ChatWindow,
    val chatTab: ChatTab,
    val chatPlusGuiMessageLine: ChatTab.ChatPlusGuiMessageLine,
    var returnFunction: Boolean = false
) : Event

data class ChatTabGetMessageAtEvent(
    val chatWindow: ChatWindow,
    val chatTab: ChatTab,
    var chatX: Double,
    var chatY: Double,
) : Event

data class ChatTabRescale(
    val chatWindow: ChatWindow,
    val chatTab: ChatTab
) : Event

data class ChatTabRewrapDisplayMessages(
    val chatWindow: ChatWindow,
    val chatTab: ChatTab,
) : Event

data class ChatTabRefreshDisplayMessages(
    val chatWindow: ChatWindow,
    val chatTab: ChatTab,
    val rescale: Boolean,
    val predicates: MutableList<Predicate<ChatTab.ChatPlusGuiMessage>> = mutableListOf(),
) : Event

@Serializable
class ChatTab : MessageFilter {

    data class ChatPlusGuiMessage(
        var rawComponent: Component?,
        val guiMessage: GuiMessage,
        var timesRepeated: Int = 1,
        var senderUUID: UUID? = null
    )

    data class ChatPlusGuiMessageLine(
        val line: GuiMessage.Line,
        val content: String,
        val linkedMessage: ChatPlusGuiMessage,
        val wrappedIndex: Int
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

    constructor(chatWindow: ChatWindow, name: String, pattern: String, autoPrefix: String = "", alwaysAdd: Boolean = false) : super(pattern) {
        this.chatWindow = chatWindow
        this.name = name
        this.autoPrefix = autoPrefix
        this.alwaysAdd = alwaysAdd
    }

    constructor(name: String, pattern: String, autoPrefix: String = "", alwaysAdd: Boolean = false) : super(pattern) {
        this.name = name
        this.autoPrefix = autoPrefix
        this.alwaysAdd = alwaysAdd
    }

    @Transient
    val messages: MutableList<ChatPlusGuiMessage> = ArrayList()

    @Transient
    var displayedMessages: MutableList<ChatPlusGuiMessageLine> = ArrayList()

    // represents displayMessages before any filter is applied, used to revert back instantly + cleared as well
    @Transient
    var unfilteredDisplayedMessages: MutableList<ChatPlusGuiMessageLine> = ArrayList()

    @Transient
    var wasFiltered = false

    @Transient
    var refreshing = false

    @Transient
    var rescaleChat = false

    @Transient
    var filterChat = false

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
    var yStart: Double = 0.0

    @Transient
    var lastMessageTime: Long = 0

    @Transient
    lateinit var chatWindow: ChatWindow


    fun addNewMessage(
        component: Component,
        signature: MessageSignature?,
        addedTime: Int,
        tag: GuiMessageTag?
    ) {
        val componentWithTimeStamp: MutableComponent = getTimeStampedMessage(component)
        val chatPlusGuiMessage = ChatPlusGuiMessage(
            if (Config.values.compactMessagesIgnoreTimestamps) component else null,
            GuiMessage(addedTime, componentWithTimeStamp, signature, tag)
        )
        if (EventBus.post(
                ChatTabAddNewMessageEvent(
                    chatWindow,
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
        this.lastMessageTime = System.currentTimeMillis()
        while (this.messages.size > Config.values.maxMessages) {
            EventBus.post(ChatTabRemoveMessageEvent(chatWindow, this, this.messages.removeFirst()))
        }
        this.addNewDisplayMessage(componentWithTimeStamp, addedTime, tag, chatPlusGuiMessage)
    }

    private fun getTimeStampedMessage(component: Component): MutableComponent {
        if (Config.values.chatTimestampMode == TimestampMode.NONE) {
            return component.copy() as MutableComponent
        }
        val componentWithTimeStamp: MutableComponent = Component.empty()
        val timestampedHoverComponents = HashSet<Any>()
        component.toFlatList().forEach {
            val flatComponent = it as MutableComponent
            if (flatComponent.style.hoverEvent == null) {
                flatComponent.withStyle {
                    val hoverValue = getTimestamp(false)
                    timestampedHoverComponents.add(hoverValue)
                    it.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverValue))
                }
            } else {
                when (flatComponent.style.hoverEvent?.action) {
                    HoverEvent.Action.SHOW_TEXT -> {
                        val hoverValue = flatComponent.style.hoverEvent?.getValue(HoverEvent.Action.SHOW_TEXT)
                        if (hoverValue != null && !timestampedHoverComponents.containsReference(hoverValue)) {
                            hoverValue.siblings.add(getTimestamp(true))
                            timestampedHoverComponents.add(hoverValue)
                        }
                    }

                    HoverEvent.Action.SHOW_ENTITY -> {
                        val hoverValue = flatComponent.style.hoverEvent?.getValue(HoverEvent.Action.SHOW_ENTITY)
                        if (hoverValue != null && !timestampedHoverComponents.containsReference(hoverValue.tooltipLines)) {
                            hoverValue.tooltipLines.add(getTimestamp(false))
                            timestampedHoverComponents.add(hoverValue.tooltipLines)
                        }
                    }
                }

            }
            componentWithTimeStamp.append(flatComponent)
        }
        return componentWithTimeStamp
    }

    fun addNewDisplayMessage(
        component: MutableComponent,
        addedTime: Int,
        tag: GuiMessageTag?,
        linkedMessage: ChatPlusGuiMessage
    ) {
        val maxWidth = Mth.floor(this.chatWindow.renderer.getBackgroundWidth())
        val displayMessageEvent =
            EventBus.post(ChatTabAddDisplayMessageEvent(chatWindow, this, component, addedTime, tag, linkedMessage, maxWidth))
        addWrappedComponents(component, displayMessageEvent, addedTime, tag, linkedMessage, -1)
        while (
            !displayMessageEvent.filtered &&
            displayMessageEvent.addMessage &&
            this.displayedMessages.isNotEmpty() &&
            this.messages[0] !== this.displayedMessages[0].linkedMessage
        ) {
            EventBus.post(ChatTabRemoveDisplayMessageEvent(chatWindow, this, this.displayedMessages.removeFirst()))
            if (wasFiltered) {
                unfilteredDisplayedMessages.removeFirst()
            }
        }
        while (this.unfilteredDisplayedMessages.isNotEmpty() && this.messages[0] !== this.unfilteredDisplayedMessages[0].linkedMessage) {
            unfilteredDisplayedMessages.removeFirst()
        }
    }

    fun addWrappedComponents(
        component: MutableComponent,
        displayMessageEvent: ChatTabAddDisplayMessageEvent,
        addedTime: Int,
        tag: GuiMessageTag?,
        linkedMessage: ChatPlusGuiMessage,
        index: Int
    ) {
        val list: List<Pair<FormattedCharSequence, String>> = wrapComponents(
            component,
            displayMessageEvent.maxWidth,
            Minecraft.getInstance().font
        )
        var wrappedIndex = index
        for (j in list.indices) {
            val chatPlusLine = list[j]
            val formattedCharSequence = chatPlusLine.first
            val content = chatPlusLine.second
            if (ChatManager.isChatFocused() && chatScrollbarPos > 0) {
                newMessageSinceScroll = true
                scrollChat(1)
            }
            val lastComponent = j == list.size - 1
            val line = ChatPlusGuiMessageLine(
                GuiMessage.Line(addedTime, formattedCharSequence, tag, lastComponent),
                content,
                linkedMessage,
                j
            )
            if (index == -1) {
                if (displayMessageEvent.addMessage) {
                    this.displayedMessages.add(line)
                }
                this.unfilteredDisplayedMessages.add(line)
            } else {
                if (displayMessageEvent.addMessage) {
                    this.displayedMessages.add(wrappedIndex, line)
                }
                this.unfilteredDisplayedMessages.add(wrappedIndex, line)
                wrappedIndex++
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

    fun clear() {
        messages.clear()
        displayedMessages.clear()
        unfilteredDisplayedMessages.clear()
    }

    fun getHoveredOverMessageLine(): ChatPlusGuiMessageLine? {
        return getMessageLineAt(ChatPlusScreen.lastMouseX.toDouble(), ChatPlusScreen.lastMouseY.toDouble())
    }

    fun getMessageLineAt(mouseX: Double, mouseY: Double): ChatPlusGuiMessageLine? {
        val x = screenToChatX(mouseX)
        val y = screenToChatY(mouseY)
        return getMessageAtLineRelative(x, y)
    }

    fun getMessageAtLineRelative(x: Double, y: Double): ChatPlusGuiMessageLine? {
        val i = getMessageLineIndexAtRelative(x, y)
        val size = this.displayedMessages.size
        return if (i in 0 until size) {
            return this.displayedMessages[size - i - 1]
        } else {
            null
        }
    }

    private fun screenToChatX(pX: Double): Double {
        return (pX - chatWindow.renderer.internalX) / chatWindow.renderer.scale
    }

    private fun screenToChatY(pY: Double): Double {
        val yDiff: Double = chatWindow.renderer.internalY - pY
        return when (Config.values.messageDirection) {
            MessageDirection.TOP_DOWN -> chatWindow.renderer.rescaledLinesPerPage - yDiff / (chatWindow.renderer.scale * chatWindow.renderer.lineHeight.toDouble())
            MessageDirection.BOTTOM_UP -> yDiff / (chatWindow.renderer.scale * chatWindow.renderer.lineHeight.toDouble())
        }
    }

    private fun getMessageLineIndexAt(pMouseX: Double, pMouseY: Double): Int {
        return getMessageLineIndexAtRelative(screenToChatX(pMouseX), screenToChatY(pMouseY))
    }

    private fun getMessageLineIndexAtRelative(pMouseX: Double, pMouseY: Double): Int {
        if (!ChatManager.isChatFocused() || Minecraft.getInstance().options.hideGui) {
            return -1
        }
        if (!(0.0 <= pMouseX && pMouseX <= Mth.floor(chatWindow.renderer.rescaledWidth.toDouble()))) {
            return -1
        }
        val i = min(chatWindow.renderer.rescaledLinesPerPage, this.displayedMessages.size)
        if (!(0.0 <= pMouseY && pMouseY < i.toDouble())) {
            return -1
        }
        val j = Mth.floor(pMouseY + chatScrollbarPos.toDouble())
        if (j < 0 || j >= this.displayedMessages.size) {
            return -1
        }
        return j
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
        val maxScroll = displayedMessages.size - chatWindow.renderer.getLinesPerPageScaled()
        if (pos > maxScroll) {
            pos = maxScroll
        }
        if (pos <= 0) {
            pos = 0
            this.newMessageSinceScroll = false
        }
        chatScrollbarPos = pos
    }

    fun moveToMessage(chatScreen: ChatScreen, message: ChatPlusGuiMessageLine) {
        val linkedMessage = message.linkedMessage
        val moveIndex = when (Config.values.jumpToMessageMode) {
            JumpToMessageMode.TOP -> this.chatWindow.renderer.rescaledLinesPerPage
            JumpToMessageMode.MIDDLE -> this.chatWindow.renderer.getLinesPerPageScaled() / 2 + 1
            JumpToMessageMode.BOTTOM -> 1
            JumpToMessageMode.CURSOR -> getMessageLineIndexAt(
                ChatPlusScreen.lastMouseX.toDouble(),
                ChatPlusScreen.lastMouseY.toDouble()
            ) - chatScrollbarPos + 1
        }
        rescaleChat = false
        filterChat = true
        refreshDisplayMessages()
        (chatScreen as IMixinScreen).callRebuildWidgets()
        val displayIndex = ChatManager.globalSelectedTab.displayedMessages.indexOfFirst { line -> line.linkedMessage === linkedMessage }
        val scrollTo = ChatManager.globalSelectedTab.displayedMessages.size - displayIndex - moveIndex
        ChatManager.globalSelectedTab.scrollChat(scrollTo)
    }

    fun rescaleChat() {
        ChatPlus.LOGGER.info("Rescale chat")
        EventBus.post(ChatTabRescale(chatWindow, this))
        resetChatScroll()
        queueRefreshDisplayedMessages(true)
        this.chatWindow.renderer.updateCachedDimension()
    }

    fun queueRefreshDisplayedMessages(reason: Boolean) {
        ChatPlus.LOGGER.info("Queueing refresh - $reason")
        if (reason) {
            rescaleChat = true
            resetDisplayMessageAtTick = Events.currentTick + 20
        } else {
            filterChat = true
            resetDisplayMessageAtTick = Events.currentTick + 15
        }
    }

    fun refreshDisplayMessages() {
        if (refreshing) {
            ChatPlus.LOGGER.info("Next refreshing")
            queueRefreshDisplayedMessages(rescaleChat)
            return
        }
        refreshing = true
        resetDisplayMessageAtTick = -1

        val start = System.currentTimeMillis()

        if (rescaleChat) {
            ChatPlus.LOGGER.info("Rewrapping messages")
            EventBus.post(ChatTabRewrapDisplayMessages(chatWindow, this))

            rescaleChat = false

            displayedMessages.clear()
            unfilteredDisplayedMessages.clear()
            for (i in messages.indices) {
                val chatPlusGuiMessage: ChatPlusGuiMessage = messages[i]
                val guiMessage: GuiMessage = chatPlusGuiMessage.guiMessage
                this.addNewDisplayMessage(
                    guiMessage.content() as MutableComponent,
                    guiMessage.addedTime(),
                    guiMessage.tag(),
                    chatPlusGuiMessage
                )
            }
            wasFiltered = false
            ChatPlus.LOGGER.info("Added ${displayedMessages.size} messages")
            ChatPlus.LOGGER.info("Rewrapping time taken: ${System.currentTimeMillis() - start}ms")
        } else if (filterChat) {
            val filterStart = System.currentTimeMillis()
            filterChat = false
            val refreshEvent = EventBus.post(ChatTabRefreshDisplayMessages(chatWindow, this, false))
            val filters = refreshEvent.predicates
            if (filters.isEmpty()) {
                resetFilter()
            } else {
                ChatPlus.LOGGER.info("Filtering - $wasFiltered")
                if (!wasFiltered) {
                    unfilteredDisplayedMessages = displayedMessages.toMutableList()
                    ChatPlus.LOGGER.info("Saved ${unfilteredDisplayedMessages.size} messages")
                } else {
                    displayedMessages = unfilteredDisplayedMessages.toMutableList()
                    ChatPlus.LOGGER.info("Loaded ${displayedMessages.size} messages")
                }
                wasFiltered = true
                val oldDisplayedMessageSize = displayedMessages.size
                val numberOfThreads = Runtime.getRuntime().availableProcessors()
                val chunked = displayedMessages.chunked((displayedMessages.size / numberOfThreads) + 1)
                ChatPlus.LOGGER.info("Chunked into ${chunked.size} chunks")
                val threads = arrayOfNulls<Thread>(numberOfThreads)
                val threadMessages: MutableMap<Int, MutableList<ChatPlusGuiMessageLine>> = mutableMapOf()
                val matchedMessages: KeySetView<ChatPlusGuiMessage, Boolean> = ConcurrentHashMap.newKeySet()
                chunked.forEachIndexed { index, chatPlusGuiMessageLines ->
                    threads[index] = Thread {
                        val localFiltered: MutableList<ChatPlusGuiMessageLine> = mutableListOf()
                        chatPlusGuiMessageLines.forEach {
                            val chatPlusGuiMessage: ChatPlusGuiMessage = it.linkedMessage
                            if (matchedMessages.contains(chatPlusGuiMessage) || filters.all { filter -> filter.test(chatPlusGuiMessage) }) {
                                matchedMessages.add(chatPlusGuiMessage)
                                localFiltered.add(it)
                            }
                        }
                        synchronized(threadMessages) {
                            threadMessages[index] = localFiltered
                        }
                    }
                    threads[index]!!.start()
                }
                threads.forEach {
                    it?.join()
                }
                val newMessages = displayedMessages.subList(oldDisplayedMessageSize, displayedMessages.size)
                ChatPlus.LOGGER.info("New messages: ${newMessages.size} - $oldDisplayedMessageSize - ${displayedMessages.size}")
                displayedMessages.clear()
                threadMessages.toSortedMap().forEach { (_, value) ->
                    displayedMessages.addAll(value)
                }
            }
            ChatPlus.LOGGER.info("Filter time taken: ${System.currentTimeMillis() - filterStart}ms")
        }
        resetChatScroll()
//        ChatPlus.LOGGER.info("Refresh time taken: ${System.currentTimeMillis() - start}ms")

        refreshing = false
    }

    fun resetFilter() {
        ChatPlus.LOGGER.info("Reset Filter -  $wasFiltered")
        if (wasFiltered) {
            if (unfilteredDisplayedMessages.size < 100) {
                ChatPlus.LOGGER.error("NO MESSAGES")
            }
            displayedMessages = unfilteredDisplayedMessages.toMutableList()
            unfilteredDisplayedMessages.clear()
            wasFiltered = false
            ChatPlus.LOGGER.info("Reloaded ${displayedMessages.size} messages")
        }
    }

    fun getComponentStyleAt(mouseX: Double, mouseY: Double): Style? {
        val messageAtEvent = EventBus.post(
            ChatTabGetMessageAtEvent(
                chatWindow,
                this,
                screenToChatX(mouseX),
                screenToChatY(mouseY)
            )
        )
        val x = messageAtEvent.chatX
        val y = messageAtEvent.chatY
        val i = getMessageLineIndexAtRelative(x, y)
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
        const val TAB_HEIGHT = 9 + PADDING * 2

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