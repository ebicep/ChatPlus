package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.JumpToMessageMode
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.features.internal.MessageFilterFormatted
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatPlusScreen
import com.ebicep.chatplus.mixin.IMixinScreen
import com.google.common.base.Predicate
import com.google.common.collect.Lists
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
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
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentHashMap.KeySetView

object ChatTabSerializer : KSerializer<MutableList<ChatTab>> {

    override val descriptor: SerialDescriptor = ListSerializer(ChatTab.serializer()).descriptor

    override fun serialize(encoder: Encoder, value: MutableList<ChatTab>) {
        val filteredItems = value.filter { !it.temporary }
        ListSerializer(ChatTab.serializer()).serialize(encoder, filteredItems)
    }

    override fun deserialize(decoder: Decoder): MutableList<ChatTab> {
        return ListSerializer(ChatTab.serializer()).deserialize(decoder).toMutableList()
    }
}

@Serializable
class ChatTab : MessageFilterFormatted {

    class ChatPlusGuiMessage(
        var timesRepeated: Int = 1,
        var senderUUID: UUID? = null
    ) {
        lateinit var guiMessage: GuiMessage
    }

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

    var commandsOverrideAutoPrefix: Boolean = true

    var temporary = false

    @Transient
    var isAutoTab = false

    constructor(
        chatWindow: ChatWindow,
        name: String,
        pattern: String,
        autoPrefix: String = "",
        priority: Int = 0,
        alwaysAdd: Boolean = false,
        skipOthers: Boolean = false,
        commandsOverrideAutoPrefix: Boolean = true,
        temporary: Boolean = false,
        isAutoTab: Boolean = false
    ) : super(pattern) {
        this.chatWindow = chatWindow
        this.name = name
        this.autoPrefix = autoPrefix
        this.priority = priority
        this.alwaysAdd = alwaysAdd
        this.skipOthers = skipOthers
        this.commandsOverrideAutoPrefix = commandsOverrideAutoPrefix
        this.temporary = temporary
        this.isAutoTab = isAutoTab
    }

    constructor(name: String, pattern: String, autoPrefix: String = "", alwaysAdd: Boolean = false) : super(pattern) {
        this.name = name
        this.autoPrefix = autoPrefix
        this.alwaysAdd = alwaysAdd
    }

    constructor(pattern: String, formatted: Boolean) : super(pattern, formatted) {
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
    var xStart: Int = 0
        set(value) {
            field = value
            xEnd = field + width
        }

    @Transient
    var xEnd: Int = 0

    @Transient
    var yStart: Int = 0

    @Transient
    var lastMessageTime: Long = 0

    @Transient
    var read: Boolean = true


    @Transient
    lateinit var chatWindow: ChatWindow

    override fun toString(): String {
        return "ChatTab($name)"
    }

    fun addNewMessage(addNewMessageEvent: AddNewMessageEvent) {
        val mutableComponent = addNewMessageEvent.mutableComponent.copy()
        val rawComponent = addNewMessageEvent.rawComponent
        val signature = addNewMessageEvent.signature
        val addedTime = addNewMessageEvent.addedTime
        val tag = addNewMessageEvent.tag
        val chatPlusGuiMessage = ChatPlusGuiMessage(senderUUID = addNewMessageEvent.senderUUID)
        if (EventBus.post(
                ChatTabAddNewMessageEvent(
                    chatWindow,
                    this,
                    chatPlusGuiMessage,
                    mutableComponent,
                    rawComponent,
                    signature,
                    addedTime,
                    tag,
                )
            ).returnFunction
        ) {
            return
        }
        chatPlusGuiMessage.guiMessage = GuiMessage(addedTime, mutableComponent, signature, tag)
        this.messages.add(chatPlusGuiMessage)
        this.lastMessageTime = System.currentTimeMillis()
        if (Config.values.maxMessages > 0) {
            while (this.messages.size > Config.values.maxMessages) {
                EventBus.post(ChatTabRemoveMessageEvent(chatWindow, this, this.messages.removeFirst()))
            }
        }
        this.addNewDisplayMessage(mutableComponent, addedTime, tag, chatPlusGuiMessage)
        if (chatWindow.tabSettings.selectedTab != this) {
            this.read = false
        }
    }

    private fun addNewDisplayMessage(
        component: MutableComponent,
        addedTime: Int,
        tag: GuiMessageTag?,
        linkedMessage: ChatPlusGuiMessage
    ) {
        val maxWidth = Mth.floor(this.chatWindow.renderer.getBackgroundWidth())
        val displayMessageEvent = EventBus.post(
            ChatTabAddDisplayMessageEvent(
                AddDisplayMessageType.TAB,
                chatWindow,
                this,
                component,
                addedTime,
                tag,
                linkedMessage,
                maxWidth
            )
        )
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
                // check if index is valid
                if (displayMessageEvent.addMessage && wrappedIndex in 0..<displayedMessages.size) {
                    this.displayedMessages.add(wrappedIndex, line)
                }
                if (wrappedIndex in 0..<unfilteredDisplayedMessages.size) {
                    this.unfilteredDisplayedMessages.add(wrappedIndex, line)
                }
                wrappedIndex++
            }
        }
    }

    fun clear() {
        messages.clear()
        displayedMessages.clear()
        unfilteredDisplayedMessages.clear()
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
            JumpToMessageMode.CURSOR -> ChatPositionTranslator.getMessageLineIndexAt(
                this,
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
        ChatPlus.LOGGER.info("$this Rescale")
        EventBus.post(ChatTabRescale(chatWindow, this))
        resetChatScroll()
        queueRefreshDisplayedMessages(true)
    }

    fun queueRefreshDisplayedMessages(reason: Boolean) {
        ChatPlus.LOGGER.info("$this Queueing refresh - $reason")
        if (reason) {
            rescaleChat = true
            resetDisplayMessageAtTick = Events.currentTick + (if (!isSelectedInAnyWindow()) 60 else 20)
        } else {
            filterChat = true
            resetDisplayMessageAtTick = Events.currentTick + 15
        }
    }

    private fun isSelectedInAnyWindow(): Boolean {
        return Config.values.chatWindows.any { it.tabSettings.selectedTab == this }
    }

    fun refreshDisplayMessages() {
        if (refreshing) {
            ChatPlus.LOGGER.info("$this Next refreshing")
            queueRefreshDisplayedMessages(rescaleChat)
            return
        }
        refreshing = true
        resetDisplayMessageAtTick = -1

        val start = System.currentTimeMillis()

        if (rescaleChat) {
            ChatPlus.LOGGER.info("$this Rewrapping messages")
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
            ChatPlus.LOGGER.info("$this Added ${displayedMessages.size} messages")
            ChatPlus.LOGGER.info("$this Rewrapping time taken: ${System.currentTimeMillis() - start}ms")
        } else if (filterChat) {
            val filterStart = System.currentTimeMillis()
            filterChat = false
            val refreshEvent = EventBus.post(ChatTabRefreshDisplayMessages(chatWindow, this, false))
            val filters = refreshEvent.predicates
            if (filters.isEmpty()) {
                resetFilter()
            } else {
                ChatPlus.LOGGER.info("$this Filtering - $wasFiltered")
                if (!wasFiltered) {
                    unfilteredDisplayedMessages = displayedMessages.toMutableList()
                    ChatPlus.LOGGER.info("$this Saved ${unfilteredDisplayedMessages.size} messages")
                } else {
                    displayedMessages = unfilteredDisplayedMessages.toMutableList()
                    ChatPlus.LOGGER.info("$this Loaded ${displayedMessages.size} messages")
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
                ChatPlus.LOGGER.info("$this New messages: ${newMessages.size} - $oldDisplayedMessageSize - ${displayedMessages.size}")
                displayedMessages.clear()
                threadMessages.toSortedMap().forEach { (_, value) ->
                    displayedMessages.addAll(value)
                }
            }
            ChatPlus.LOGGER.info("$this Filter time taken: ${System.currentTimeMillis() - filterStart}ms")
        }
        resetChatScroll()
//        ChatPlus.LOGGER.info("Refresh time taken: ${System.currentTimeMillis() - start}ms")

        refreshing = false
    }

    fun resetFilter() {
        ChatPlus.LOGGER.info("$this Reset Filter -  $wasFiltered")
        if (wasFiltered) {
            if (unfilteredDisplayedMessages.size < 100) {
                ChatPlus.LOGGER.error("$this NO MESSAGES")
            }
            displayedMessages = unfilteredDisplayedMessages.toMutableList()
            unfilteredDisplayedMessages.clear()
            wasFiltered = false
            ChatPlus.LOGGER.info("$this Reloaded ${displayedMessages.size} messages")
        }
    }

    fun getHoveredOverMessageLine(): ChatPlusGuiMessageLine? {
        return ChatPositionTranslator.getHoveredOverMessageLineInternal(this)
    }

    fun getHoveredOverMessageLine(mouseX: Double, mouseY: Double): ChatPlusGuiMessageLine? {
        return ChatPositionTranslator.getHoveredOverMessageLineInternal(this, mouseX, mouseY)
    }

    fun getComponentStyleAt(mouseX: Double, mouseY: Double): Style? {
        return ChatPositionTranslator.getComponentStyleAt(this, mouseX, mouseY)
    }

    companion object {

        const val PADDING = 2
        const val TAB_HEIGHT = 9 + PADDING * 2
        private val INDENT: FormattedCharSequence = FormattedCharSequence.codepoint(32, Style.EMPTY)

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
            val indent = Config.values.wrappedMessageLineIndent // todo fix wrapping issue
            font.splitter.splitLines(
                componentCollector.resultOrEmpty,
                maxWidth,
                Style.EMPTY
            ) { formattedText: FormattedText, isNewLine: Boolean ->
                val formattedCharSequence = Language.getInstance().getVisualOrder(formattedText)
                if (indent > 0 && isNewLine) {
                    val sequenceList: MutableList<FormattedCharSequence> = mutableListOf()
                    for (i in 0 until indent) {
                        sequenceList.add(INDENT)
                    }
                    sequenceList.add(formattedCharSequence)
                    list.add(Pair(FormattedCharSequence.composite(sequenceList), formattedText.string))
                } else {
                    // note: removed new line indent
                    list.add(Pair(formattedCharSequence, formattedText.string))
                }
            }
            return if (list.isEmpty()) {
                mutableListOf(Pair(FormattedCharSequence.EMPTY, ""))
            } else {
                list
            }
        }
    }

}

data class SkipNewMessageEvent(
    var mutableComponent: MutableComponent,
    val rawComponent: Component,
    var senderUUID: UUID?,
    val signature: MessageSignature?,
    val addedTime: Int,
    val tag: GuiMessageTag?
) : Event

data class AddNewMessageEvent(
    var mutableComponent: MutableComponent,
    val rawComponent: Component,
    var senderUUID: UUID?,
    val signature: MessageSignature?,
    val addedTime: Int,
    val tag: GuiMessageTag?,
    var returnFunction: Boolean = false
) : Event

data class ChatTabAddNewMessageEvent(
    val chatWindow: ChatWindow,
    val chatTab: ChatTab,
    val chatPlusGuiMessage: ChatTab.ChatPlusGuiMessage,
    var mutableComponent: MutableComponent,
    val rawComponent: Component,
    val signature: MessageSignature?,
    val addedTime: Int,
    val tag: GuiMessageTag?,
    var returnFunction: Boolean = false
) : Event

data class ChatTabAddDisplayMessageEvent(
    val addDisplayMessageType: AddDisplayMessageType,
    val chatWindow: ChatWindow,
    val chatTab: ChatTab,
    val component: MutableComponent,
    val addedTime: Int,
    val tag: GuiMessageTag?,
    val linkedMessage: ChatTab.ChatPlusGuiMessage,
    var maxWidth: Int,
    var addMessage: Boolean = true,
    var filtered: Boolean = false,
) : Event

enum class AddDisplayMessageType {
    TAB,
    COMPACT,
}

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
