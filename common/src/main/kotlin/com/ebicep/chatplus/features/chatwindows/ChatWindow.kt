package com.ebicep.chatplus.features.chatwindows

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.MessageDirection
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.AlignMessage
import com.ebicep.chatplus.features.ChatPadding
import com.ebicep.chatplus.features.chattabs.*
import com.ebicep.chatplus.features.chattabs.ChatTab.Companion.TAB_HEIGHT
import com.ebicep.chatplus.features.chattabs.ChatTabs.DefaultTab
import com.ebicep.chatplus.features.internal.Debug
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderer
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import com.ebicep.chatplus.util.KotlinUtil.reduceAlpha
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth
import java.awt.Color

@Serializable
class ChatWindow {

    var backgroundColor: Int = Color(0f, 0f, 0f, .5f).rgb
    var unfocusedBackgroundColorOpacityMultiplier: Float = .4f
    var outline: ChatWindowOutline = ChatWindowOutline()
    var scale: Float = 1f
    var textOpacity: Float = 1f
    var unfocusedTextOpacityMultiplier: Float = 1f
    var unfocusedHeight: Float = .5f
    var lineSpacing: Float = 0f
    var messageAlignment: AlignMessage.Alignment = AlignMessage.Alignment.LEFT
    var messageDirection: MessageDirection = MessageDirection.BOTTOM_UP
    var padding: ChatPadding.Padding = ChatPadding.Padding()
    val renderer = ChatRenderer()
    var selectedTabIndex = 0
    var startRenderTabIndex = 0
    var hideTabs = false
    var showTabsWhenChatNotOpen: Boolean = false
    var tabTextColorSelected: Int = Color(255, 255, 255, 255).rgb
    var tabTextColorUnselected: Int = Color(153, 153, 153, 255).rgb
    var unfocusedTabOpacityMultiplier: Float = .4f
    var tabs: MutableList<ChatTab> = mutableListOf()
        set(value) {
            field = value
            resetSortedChatTabs()
        }

    val selectedTab: ChatTab
        get() = tabs[selectedTabIndex]

    @Transient
    var sortedTabs: List<ChatTab> = listOf()

    init {
        ChatPlus.LOGGER.info("Create $this")
        // correct values
        scale = Mth.clamp(scale, 0f, 1f)
        textOpacity = Mth.clamp(textOpacity, 0f, 1f)
        unfocusedHeight = Mth.clamp(unfocusedHeight, 0f, 1f)
        lineSpacing = Mth.clamp(lineSpacing, 0f, 1f)
        if (tabs.isEmpty()) {
            tabs.add(DefaultTab)
        }
        selectedTabIndex = Mth.clamp(selectedTabIndex, 0, tabs.size - 1)

        tabs.forEach {
            it.chatWindow = this
            it.regex = Regex(it.pattern)
        }
        renderer.chatWindow = this

        resetSortedChatTabs()
    }

    /**
     * Returns a clone ChatWindow matching the visual properties of this ChatWindow. Excluding renderer and tabs.
     */
    fun clone(): ChatWindow {
        return ChatWindow().also {
            it.backgroundColor = backgroundColor
            it.outline = outline.clone()
            it.scale = scale
            it.textOpacity = textOpacity
            it.unfocusedHeight = unfocusedHeight
            it.unfocusedBackgroundColorOpacityMultiplier = unfocusedBackgroundColorOpacityMultiplier
            it.lineSpacing = lineSpacing
            it.messageAlignment = messageAlignment
            it.messageDirection = messageDirection
            it.padding = padding.clone()
            it.hideTabs = hideTabs
            it.unfocusedTabOpacityMultiplier = unfocusedTabOpacityMultiplier
        }
    }

    override fun toString(): String {
        return "ChatWindow(${tabs.joinToString(",") { it.name }})"
    }

    fun resetSortedChatTabs() {
        sortedTabs = tabs.sortedBy { -it.priority }
    }

    // negative = scroll left , positive = scroll right
    fun scrollTab(amount: Int) {
        if (amount < 0 && startRenderTabIndex > 0) {
            startRenderTabIndex--
        } else if (amount > 0 && startRenderTabIndex < tabs.size - 1) {
            // check if last tab is visible
            var totalWidth = 0
            tabs.forEachIndexed { index, it ->
                if (index < startRenderTabIndex) {
                    return@forEachIndexed
                }
                totalWidth += it.width + CHAT_TAB_X_SPACE
            }
            if (totalWidth >= Minecraft.getInstance().window.guiScaledWidth) {
                startRenderTabIndex++
            }
        }
        if (Config.values.moveToTabWhenCycling) {
            selectedTabIndex = Mth.clamp(selectedTabIndex + amount, 0, tabs.size - 1)
        }
    }

    fun handleClickedTab(x: Double, y: Double) {
        val clickedTab: ChatTab = getClickedTab(x, y) ?: return
        EventBus.post(ChatTabClickedEvent(clickedTab, x, y, clickedTab.xStart.toDouble(), clickedTab.yStart.toDouble()))
        if (clickedTab != ChatManager.globalSelectedTab) {
            val oldTab = ChatManager.globalSelectedTab
            oldTab.resetFilter()
            selectedTabIndex = tabs.indexOf(clickedTab)
            queueUpdateConfig = true
            ChatManager.globalSelectedTab.queueRefreshDisplayedMessages(false)
            EventBus.post(ChatTabSwitchEvent(oldTab, clickedTab))
        }
    }

    fun getClickedTab(x: Double, y: Double): ChatTab? {
        val translatedY = renderer.getUpdatedY() - y
        if (translatedY > CHAT_TAB_Y_OFFSET - 4 || translatedY < -(9 + ChatTab.PADDING + ChatTab.PADDING)) {
            return null
        }
        tabs.forEachIndexed { index, it ->
            if (index < startRenderTabIndex) {
                return@forEachIndexed
            }
            val insideTabX = it.xStart < x && x < it.xEnd
            if (insideTabX) {
                return it
            }
        }
        return null
    }

    fun renderTabs(guiGraphics: GuiGraphics) {
        val poseStack = guiGraphics.pose()
        var xStart: Int = renderer.internalX
        val yStart: Int = renderer.internalY + CHAT_TAB_Y_OFFSET
        poseStack.createPose {
            tabs.forEachIndexed { index, it ->
                if (index < startRenderTabIndex) {
                    return@forEachIndexed
                }
                poseStack.createPose {
                    val tabWidth = it.width

                    val renderEvent = EventBus.post(ChatTabRenderEvent(guiGraphics, it, tabWidth, xStart, yStart))
                    val translateX = renderEvent.xStart
                    val translatedY = renderEvent.yStart
                    poseStack.translate0(x = translateX, y = translatedY)

                    it.xStart = translateX
                    it.yStart = translatedY

                    if (!hideTabs) {
                        renderTab(it, guiGraphics)
                    }

                    xStart += tabWidth + CHAT_TAB_X_SPACE

                    if (Debug.debug) {
                        poseStack.createPose {
                            poseStack.guiForward()
                            guiGraphics.drawString(
                                Minecraft.getInstance().font,
                                "x:${it.xStart}",
                                0,
                                -20,
                                0xFF5050
                            )
                            guiGraphics.drawString(
                                Minecraft.getInstance().font,
                                "y:${it.yStart}",
                                0,
                                -10,
                                0xFF5050
                            )
                        }
                    }
                }
            }
        }
    }

    private fun renderTab(chatTab: ChatTab, guiGraphics: GuiGraphics) {
        val poseStack = guiGraphics.pose()
        val isGlobalSelected = chatTab == ChatManager.globalSelectedTab
        val isWindowSelected = this == ChatManager.selectedWindow
        val chatWindow = chatTab.chatWindow
        var backgroundColor = chatWindow.backgroundColor
        if (!isWindowSelected) {
            backgroundColor = reduceAlpha(backgroundColor, unfocusedTabOpacityMultiplier)
        }
        var textColor = if (isGlobalSelected) chatWindow.tabTextColorSelected else tabTextColorUnselected
        if (!isWindowSelected) {
            textColor = reduceAlpha(textColor, if (unfocusedTabOpacityMultiplier == 0f) .05f else unfocusedTextOpacityMultiplier)
        }
        val startY = if (isWindowSelected && chatTab == selectedTab) {
            if (chatWindow.outline.outlineTabType == ChatWindowOutline.OutlineTabType.SELECTED_TAB_OPEN_TOP) {
                -(chatTab.yStart - chatWindow.renderer.internalY)
            } else {
                -CHAT_TAB_Y_OFFSET
            }
        } else {
            0
        }

        poseStack.createPose {
            poseStack.guiForward(if (isGlobalSelected) 650.0 else 550.0)
            guiGraphics.fill(
                0,
                startY,
                chatTab.width,
                TAB_HEIGHT,
                backgroundColor
            )
            poseStack.guiForward()
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                chatTab.name,
                ChatTab.PADDING,
                ChatTab.PADDING + ChatTab.PADDING / 2,
                textColor
            )
        }
    }

    fun getTabBarWidth(): Int {
        var totalWidth = 0
        tabs.forEachIndexed { index, it ->
            if (index < startRenderTabIndex) {
                return@forEachIndexed
            }
            totalWidth += it.width + CHAT_TAB_X_SPACE
        }
        return totalWidth - CHAT_TAB_X_SPACE
    }

    fun getUpdatedBackgroundColor(): Int {
        if (this == ChatManager.selectedWindow) {
            return backgroundColor
        }
        return reduceAlpha(backgroundColor, unfocusedBackgroundColorOpacityMultiplier)
    }

    fun getUpdatedTextOpacity(): Float {
        if (this == ChatManager.selectedWindow) {
            return textOpacity
        }
        return textOpacity * unfocusedTextOpacityMultiplier
    }

}