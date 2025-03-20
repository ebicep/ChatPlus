package com.ebicep.chatplus.features

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.IChatScreen
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.EnumTranslatableName
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.FindMessage.FindMode.CONTAINS
import com.ebicep.chatplus.features.FindMessage.FindMode.REGEX
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.ChatTabAddDisplayMessageEvent
import com.ebicep.chatplus.features.chattabs.ChatTabRefreshDisplayMessages
import com.ebicep.chatplus.features.chattabs.ChatTabRewrapDisplayMessages
import com.ebicep.chatplus.features.chatwindows.ChatTabSwitchEvent
import com.ebicep.chatplus.features.chatwindows.WindowSwitchEvent
import com.ebicep.chatplus.features.textbarelements.AddTextBarElementEvent
import com.ebicep.chatplus.features.textbarelements.FindTextBarElement
import com.ebicep.chatplus.features.textbarelements.FindToggleEvent
import com.ebicep.chatplus.features.textbarelements.TranslateToggleEvent
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.mixin.IMixinScreen
import com.ebicep.chatplus.util.ComponentUtil
import com.ebicep.chatplus.util.GraphicsUtil.fill0
import kotlinx.serialization.Serializable
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.awt.Color

object FindMessage {

    var findMode: FindMode? = null
    val findEnabled: Boolean
        get() = findMode != null
    var lastFindMode = CONTAINS
    private var lastInput = ""
    private var lastInputRegex = Regex("")

    init {
        var lastMovedToMessage: Pair<Pair<ChatTab.ChatPlusGuiMessage, Int>, Long>? = null // <linked message, wrapped index>, tick
        EventBus.register<AddTextBarElementEvent>({ 100 }) {
            if (!Config.values.findMessageEnabled) {
                return@register
            }
            if (Config.values.findMessageTextBarElementEnabled) {
                it.elements.add(FindTextBarElement(it.screen))
            }
        }
        var findShortcutUsed = false
        EventBus.register<ChatScreenInputEvent>({ 1 }, { findShortcutUsed }) {
            findShortcutUsed = false
            if (!Config.values.findMessageEnabled) {
                return@register
            }
            if (it.checkRelease(Config.values.findMessageKey)) {
                return@register
            }
            findShortcutUsed = true
            val shiftDown = Screen.hasShiftDown()
            toggle(it.screen, if (shiftDown) getOtherMode() else Config.values.findMessageDefaultMode)
            it.returnFunction = true
        }
        EventBus.register<ChatScreenCloseEvent> {
            if (findEnabled) {
                findMode = null
                ChatManager.globalSelectedTab.resetFilter()
            }
        }
        EventBus.register<ChatTabRewrapDisplayMessages> {
            findMode = null
            ChatManager.globalSelectedTab.resetFilter()
        }
        EventBus.register<ChatTabRefreshDisplayMessages> {
            if (findEnabled && lastInput.isNotEmpty()) {
                if (findMode == REGEX) {
                    try {
                        lastInputRegex = Regex(lastInput, if (Config.values.findMessageIgnoreCase) mutableSetOf(RegexOption.IGNORE_CASE) else emptySet())
                    } catch (_: Exception) {
                        ChatPlus.sendMessage(ComponentUtil.literal("Invalid Regex", ChatFormatting.RED))
                        return@register
                    }
                }
                it.predicates.add { guiMessage ->
                    val string = guiMessage.guiMessage.content.string
                    if (findMode == REGEX) {
                        string.contains(lastInputRegex)
                    } else {
                        string.contains(lastInput, ignoreCase = Config.values.findMessageIgnoreCase)
                    }
                }
            }
        }
        // when switching tabs/windows, refresh filter
        EventBus.register<ChatTabSwitchEvent> {
            if (findEnabled) {
                ChatManager.globalSelectedTab.queueRefreshDisplayedMessages(false)
            }
        }
        EventBus.register<WindowSwitchEvent> {
            if (findEnabled) {
                ChatManager.globalSelectedTab.queueRefreshDisplayedMessages(false)
            }
        }
        EventBus.register<ChatTabAddDisplayMessageEvent> {
            val screen = Minecraft.getInstance().screen
            if (findEnabled && screen is IMixinChatScreen) {
                it.filtered = true
                val filter = screen.input?.value
                if (filter != null && !it.component.string.contains(filter, ignoreCase = Config.values.findMessageIgnoreCase)) {
                    it.addMessage = false
                }
            }
        }
        EventBus.register<ChatScreenInputBoxEditEvent>({ 5 }, { findEnabled }) {
            if (findEnabled) {
                lastInput = it.str
                ChatManager.globalSelectedTab.queueRefreshDisplayedMessages(false)
                it.returnFunction = true
            }
        }
        EventBus.register<ChatScreenRenderEvent> {
            if (findEnabled && Config.values.findMessageHighlightInputBox) {
                it.screen as IMixinChatScreen
                it.screen as IChatScreen
                val editBox = it.screen.input ?: return@register
                it.guiGraphics.renderOutline(
                    editBox.x - 2,
                    editBox.y - (if (Config.values.vanillaInputBox) 2 else 4),
                    it.screen.chatPlusWidth - (if (Config.values.vanillaInputBox) 2 else 0),
                    editBox.height,
                    findMode!!.color
                )
            }
        }
        EventBus.register<TranslateToggleEvent> {
            if (findEnabled) {
                findMode = null
                ChatManager.globalSelectedTab.resetFilter()
            }
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            if (it.button != 0) {
                return@register
            }
            if (findEnabled) {
                ChatManager.globalSelectedTab.getHoveredOverMessageLine(it.mouseX, it.mouseY)?.let { message ->
                    val linkedMessage = message.linkedMessage
                    lastMovedToMessage = Pair(Pair(linkedMessage, message.wrappedIndex), Events.currentTick + 60)
                    findMode = null
                    ChatManager.globalSelectedTab.moveToMessage(it.screen, message)
                }
            }
        }
        EventBus.register<ChatRenderPreLineAppearanceEvent>({ Config.values.findMessageLinePriority }) {
            lastMovedToMessage?.let { message ->
                if (message.first.first !== it.chatPlusGuiMessageLine.linkedMessage ||
                    message.first.second != it.chatPlusGuiMessageLine.wrappedIndex
                ) {
                    return@let
                }
                if (message.second < Events.currentTick) {
                    return@let
                }
                it.backgroundColor = lastFindMode.backgroundColor
            }
        }
        EventBus.register<ChatRenderLineTextEvent>({ -5 }) {
            if (findEnabled && it.chatWindow.tabSettings.selectedTab.wasFiltered && Config.values.findMessageHighlightMatchedText) {
                val chatPlusGuiMessageLine = it.chatPlusGuiMessageLine
                val line = it.line
                val guiGraphics = it.guiGraphics
                val verticalChatOffset = it.verticalChatOffset
                val renderer = it.chatWindow.renderer
                val internalX = renderer.internalX
                val scale = renderer.scale
                val lineHeight = renderer.lineHeight

                val ranges =
                    if (findMode == REGEX) ComponentUtil.getWidthRanges(line.content, chatPlusGuiMessageLine.content, lastInputRegex)
                    else ComponentUtil.getWidthRange(line.content, chatPlusGuiMessageLine.content, lastInput)
                ranges.forEach {
                    guiGraphics.fill0(
                        internalX / scale + it.first,
                        verticalChatOffset - lineHeight.toFloat(),
                        internalX / scale + it.second,
                        verticalChatOffset,
                        findMode!!.color
                    )
                }

            }
        }
    }

    fun toggle(chatPlusScreen: ChatScreen, newFindMode: FindMode = Config.values.findMessageDefaultMode) {
        chatPlusScreen as IMixinChatScreen
        findMode = if (findEnabled) {
            null
        } else {
            lastFindMode = newFindMode
            newFindMode
        }
        EventBus.post(FindToggleEvent(findEnabled))
        chatPlusScreen.initial = chatPlusScreen.input!!.value
        lastInput = chatPlusScreen.input?.value ?: ""
        if (!findEnabled) {
            ChatManager.globalSelectedTab.resetFilter()
        } else {
            ChatManager.globalSelectedTab.queueRefreshDisplayedMessages(false)
        }
        chatPlusScreen as IMixinScreen
        chatPlusScreen.callRebuildWidgets()
    }

    fun getOtherMode(): FindMode {
        return if (Config.values.findMessageDefaultMode == REGEX) {
            CONTAINS
        } else {
            REGEX
        }
    }

    @Serializable
    enum class FindMode(val key: String, val color: Int, val backgroundColor: Int = Color(color).darker().rgb) : EnumTranslatableName {
        REGEX("chatPlus.findMessage.textBarElement.findMode.regex", Color(255, 200, 85, 255).rgb),
        CONTAINS("chatPlus.findMessage.textBarElement.findMode.contains", Color(255, 255, 85, 255).rgb),

        ;

        val translatable: Component = Component.translatable(key)

        override fun getTranslatableName(): Component {
            return translatable
        }
    }

}