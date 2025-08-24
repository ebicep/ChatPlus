package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.JumpToMessageMode
import com.ebicep.chatplus.config.MessageDirection
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatPlusScreen
import com.ebicep.chatplus.mixin.IMixinScreen
import com.ebicep.chatplus.util.ComponentUtil.getColoredString
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
class ChatTab {

    class ChatPlusGuiMessage(
        var timesRepeated: Int = 1,
        var senderUUID: UUID? = null,
    ) {
        lateinit var guiMessage: GuiMessage
    }

    data class ChatPlusGuiMessageLine(
        val line: GuiMessage.Line,
        val content: String,
        var coloredContent: String?,
        val linkedMessage: ChatPlusGuiMessage,
        val wrappedIndex: Int,
    ) {
        fun coloredContent(): String {
            if (coloredContent == null) {
                coloredContent = line.content.getColoredString()
            }
            return coloredContent!!
        }
    }

    var settings: MutableList<ServerChatTabSettings> = mutableListOf()

    val name: String
        get() = if (!::currentSettings.isInitialized) "" else currentSettings.name
    val autoSend: String
        get() = if (!::currentSettings.isInitialized) "" else currentSettings.autoSend
    val autoPrefix: String
        get() = if (!::currentSettings.isInitialized) "" else currentSettings.autoPrefix
    val priority: Int
        get() = if (!::currentSettings.isInitialized) 0 else currentSettings.priority
    val alwaysAdd: Boolean
        get() = if (!::currentSettings.isInitialized) false else currentSettings.alwaysAdd
    val skipOthers: Boolean
        get() = if (!::currentSettings.isInitialized) false else currentSettings.skipOthers
    val commandsOverrideAutoPrefix: Boolean
        get() = if (!::currentSettings.isInitialized) false else currentSettings.commandsOverrideAutoPrefix
    val notificationSettings: ServerChatTabNotificationSettings
        get() = if (!::currentSettings.isInitialized) ServerChatTabNotificationSettings() else currentSettings.notificationSettings

    fun matches(message: String, coloredMessage: String?): Boolean {
        return currentSettings.matches(message, coloredMessage)
    }

    fun matches(message: Component): Boolean {
        return currentSettings.matches(message)
    }

    fun matches(message: ChatPlusGuiMessageLine): Boolean {
        return currentSettings.matches(message)
    }

    @Transient
    lateinit var currentSettings: ServerChatTabSettings

    @Transient
    var temporary = false

    @Transient
    var isAutoTab = false

    constructor(
        currentSettings: ServerChatTabSettings,
    ) {
        this.currentSettings = currentSettings
    }

    constructor(
        settings: MutableList<ServerChatTabSettings>,
    ) {
        this.settings = settings
    }

    constructor(
        chatWindow: ChatWindow,
        currentSettings: ServerChatTabSettings,
    ) {
        this.chatWindow = chatWindow
        this.currentSettings = currentSettings
        this.settings = mutableListOf(currentSettings)
    }

    init {
        if (settings.isNotEmpty()) {
            this.currentSettings = settings.first()
        }
        updateServerSettings()
        updateCurrentSettings()
    }

    @Transient
    var messages: ArrayList<ChatPlusGuiMessage> = ArrayList()

    @Transient
    var displayedMessages: ArrayList<ChatPlusGuiMessageLine> = ArrayList()

    // represents displayMessages before any filter is applied, used to revert back instantly + cleared as well
    @Transient
    var unfilteredDisplayedMessages: ArrayList<ChatPlusGuiMessageLine> = ArrayList()

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
    var unreadCount: Int = 0

    @Transient
    lateinit var chatWindow: ChatWindow

    fun updateServerSettings() {
        this.settings.forEach {
            it.updateRegex()
            it.serverPattern.updateRegex()
            it.suggestionsPatterns.forEach { it.updateRegex() }
            it.notificationSettings.notificationMatch.updateRegex()
        }
    }

    fun updateCurrentSettings(ip: String? = Minecraft.getInstance().player?.connection?.serverData?.ip) {
        val patterns = "(${settings.joinToString(", ") { it.serverPattern.pattern }})"
        ChatPlus.LOGGER.info("Updating current settings for $patterns given $ip")
        var newCurrent: ServerChatTabSettings? = null
        if (ip != null) {
            settings.forEach {
                if (it.serverPattern.matches(ip)) {
                    ChatPlus.LOGGER.info("Set current settings to ${it.serverPattern.pattern} for $ip")
                    newCurrent = it
                    return@forEach
                }
            }
        }
        // sort empty server patterns first
        if (newCurrent == null) {
            newCurrent = settings.maxByOrNull { it.serverPattern.pattern.isEmpty() }
            if (newCurrent == null) {
                ChatPlus.LOGGER.error("No server pattern found for $ip in ($patterns)")
                newCurrent = ServerChatTabSettings()
                settings.add(newCurrent)
            } else {
                ChatPlus.LOGGER.info("Set current settings to default ${newCurrent.serverPattern.pattern} for $ip")
            }
        }
        newCurrent.chatTab = this
        currentSettings = newCurrent
        width = -1
        ChatPlus.LOGGER.info("Set current settings for $patterns to ${currentSettings.serverPattern.pattern} for $ip")

    }

    fun clone(): ChatTab {
        val newSettings: MutableList<ServerChatTabSettings> = this.settings.map { it.clone() }.toCollection(mutableListOf())
        return ChatTab(newSettings).also {
            it.chatWindow = chatWindow
        }
    }

    override fun toString(): String {
        return "ChatTab($name)"
    }

    fun addNewMessage(addNewMessageEvent: AddNewMessageEvent): NewMessageResult {
        val mutableComponent = addNewMessageEvent.mutableComponent.copy()
        val rawComponent = addNewMessageEvent.rawComponent
        val signature = addNewMessageEvent.signature
        val addedTime = addNewMessageEvent.addedTime
        val tag = addNewMessageEvent.tag
        val chatPlusGuiMessage = ChatPlusGuiMessage(senderUUID = addNewMessageEvent.senderUUID)
        val chatTabAddNewMessageEvent = ChatTabAddNewMessageEvent(
            addNewMessageEvent,
            chatWindow,
            this,
            chatPlusGuiMessage,
            mutableComponent,
            rawComponent,
            signature,
            addedTime,
            tag,
        )
        if (EventBus.post(chatTabAddNewMessageEvent).returnFunction) {
            return NewMessageResult(chatTabAddNewMessageEvent, null, null)
        }
        chatPlusGuiMessage.guiMessage = GuiMessage(addedTime, mutableComponent, signature, tag)
        this.messages.add(chatPlusGuiMessage)
        this.lastMessageTime = System.currentTimeMillis()
        val removedMessages = mutableListOf<ChatPlusGuiMessage>()
        if (Config.values.maxMessages > 0) {
            while (this.messages.size > Config.values.maxMessages) {
                val removed = this.messages.removeFirst()
                EventBus.post(ChatTabRemoveMessageEvent(chatWindow, this, removed))
                removedMessages.add(removed)
            }
        }
        val newDisplayMessageResult = this.addNewDisplayMessage(mutableComponent, addedTime, tag, chatPlusGuiMessage)
        if (chatWindow.tabSettings.selectedTab != this) {
            val notificationMatch = notificationSettings.notificationMatch
            if (notificationMatch.matches(rawComponent)) {
                this.unreadCount++
            }
        }
        return NewMessageResult(chatTabAddNewMessageEvent, removedMessages, newDisplayMessageResult)
    }

    private fun addNewDisplayMessage(
        component: MutableComponent,
        addedTime: Int,
        tag: GuiMessageTag?,
        linkedMessage: ChatPlusGuiMessage,
    ): NewDisplayMessageResult {
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
        val addedComponents: MutableList<ChatPlusGuiMessageLine> = addWrappedComponents(component, displayMessageEvent, addedTime, tag, linkedMessage, -1)
        val removedMessages: MutableList<ChatPlusGuiMessageLine> = mutableListOf()
        while (
            !displayMessageEvent.filtered &&
            displayMessageEvent.addMessage &&
            this.displayedMessages.isNotEmpty() &&
            this.messages[0] !== this.displayedMessages[0].linkedMessage
        ) {
            val removed = this.displayedMessages.removeFirst()
            EventBus.post(ChatTabRemoveDisplayMessageEvent(chatWindow, this, removed))
            removedMessages.add(removed)
            if (wasFiltered) {
                unfilteredDisplayedMessages.removeFirst()
            }
        }
        while (this.unfilteredDisplayedMessages.isNotEmpty() && this.messages[0] !== this.unfilteredDisplayedMessages[0].linkedMessage) {
            unfilteredDisplayedMessages.removeFirst()
        }
        return NewDisplayMessageResult(displayMessageEvent, addedComponents, removedMessages)
    }

    fun addWrappedComponents(
        component: MutableComponent,
        displayMessageEvent: ChatTabAddDisplayMessageEvent,
        addedTime: Int,
        tag: GuiMessageTag?,
        linkedMessage: ChatPlusGuiMessage,
        index: Int,
    ): MutableList<ChatPlusGuiMessageLine> {
        val list: List<Pair<FormattedCharSequence, String>> = wrapComponents(
            component,
            displayMessageEvent.maxWidth,
            Minecraft.getInstance().font
        )
        var wrappedIndex = index
        val added: MutableList<ChatPlusGuiMessageLine> = mutableListOf()

        val reversed = chatWindow.generalSettings.messageDirection == MessageDirection.TOP_DOWN &&
                chatWindow.generalSettings.topDownDirectionWrapInOrder
        val indices = if (reversed) list.indices.reversed() else list.indices
        for (j in indices) {
            val chatPlusLine = list[j]
            val formattedCharSequence = chatPlusLine.first
            val content = chatPlusLine.second
            if (chatScrollbarPos > 0) {
                newMessageSinceScroll = true
                scrollChat(1)
            }
            val lastComponent = j == list.size - 1
            val line = ChatPlusGuiMessageLine(
                GuiMessage.Line(addedTime, formattedCharSequence, tag, lastComponent),
                content,
                null,
                linkedMessage,
                j
            )
            if (index == -1) {
                if (displayMessageEvent.addMessage) {
                    this.displayedMessages.add(line)
                    added.add(line)
                }
                this.unfilteredDisplayedMessages.add(line)
            } else {
                // check if index is valid
                if (displayMessageEvent.addMessage && wrappedIndex in 0..displayedMessages.size) {
                    this.displayedMessages.add(wrappedIndex, line)
                    added.add(line)
                }
                if (wrappedIndex in 0..unfilteredDisplayedMessages.size) {
                    this.unfilteredDisplayedMessages.add(wrappedIndex, line)
                }
                wrappedIndex++
            }
        }
        return added
    }

    fun clear() {
        messages = ArrayList()
        displayedMessages = ArrayList()
        unfilteredDisplayedMessages = ArrayList()
        unreadCount = 0
    }

    fun resetChatScroll(reset: Boolean = chatWindow.generalSettings.resetScrollPositionOnClose) {
        if (reset) {
            chatScrollbarPos = 0
            this.newMessageSinceScroll = false
        }
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
        resetChatScroll(true)
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

            // recreate the list in the case of rescale from tiny to large, removes extra allocated space
            displayedMessages = ArrayList()
            unfilteredDisplayedMessages = ArrayList()
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
                    unfilteredDisplayedMessages = ArrayList(displayedMessages)
                    ChatPlus.LOGGER.info("$this Saved ${unfilteredDisplayedMessages.size} messages")
                } else {
                    displayedMessages = ArrayList(unfilteredDisplayedMessages)
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
        resetChatScroll(true)
//        ChatPlus.LOGGER.info("Refresh time taken: ${System.currentTimeMillis() - start}ms")

        refreshing = false
    }

    fun resetFilter() {
        ChatPlus.LOGGER.info("$this Reset Filter -  $wasFiltered")
        if (wasFiltered) {
            if (unfilteredDisplayedMessages.size < 100) {
                ChatPlus.LOGGER.error("$this NO MESSAGES")
            }
            displayedMessages = ArrayList(unfilteredDisplayedMessages)
            unfilteredDisplayedMessages = ArrayList()
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
        var cachedServerIPs = mutableSetOf<String>()

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

    data class NewMessageResult(
        val chatTabAddNewMessageEvent: ChatTabAddNewMessageEvent,
        val removed: MutableList<ChatPlusGuiMessage>?,
        val newDisplayMessageResult: NewDisplayMessageResult?,
    )

    data class NewDisplayMessageResult(
        val chatTabAddDisplayMessageEvent: ChatTabAddDisplayMessageEvent,
        val addedComponents: MutableList<ChatPlusGuiMessageLine>,
        val removedMessages: MutableList<ChatPlusGuiMessageLine>,
    )

}

data class SkipNewMessageEvent(
    var mutableComponent: MutableComponent,
    val rawComponent: Component,
    var senderUUID: UUID?,
    val signature: MessageSignature?,
    val addedTime: Int,
    val tag: GuiMessageTag?,
) : Event

data class AddNewMessageEvent(
    var mutableComponent: MutableComponent,
    val rawComponent: Component,
    var senderUUID: UUID?,
    val signature: MessageSignature?,
    val addedTime: Int,
    val tag: GuiMessageTag?,
    var returnFunction: Boolean = false,
) : Event

data class ChatTabAddNewMessageEvent(
    val addNewMessageEvent: AddNewMessageEvent,
    val chatWindow: ChatWindow,
    val chatTab: ChatTab,
    val chatPlusGuiMessage: ChatTab.ChatPlusGuiMessage,
    var mutableComponent: MutableComponent,
    val rawComponent: Component,
    val signature: MessageSignature?,
    val addedTime: Int,
    val tag: GuiMessageTag?,
    var returnFunction: Boolean = false,
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
    var returnFunction: Boolean = false,
) : Event

data class ChatTabRemoveDisplayMessageEvent(
    val chatWindow: ChatWindow,
    val chatTab: ChatTab,
    val chatPlusGuiMessageLine: ChatTab.ChatPlusGuiMessageLine,
    var returnFunction: Boolean = false,
) : Event

data class ChatTabRescale(
    val chatWindow: ChatWindow,
    val chatTab: ChatTab,
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
