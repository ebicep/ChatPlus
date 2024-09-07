package com.ebicep.chatplus.features.chatwindows

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_X_SPACE
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_Y_OFFSET
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.ChatTab.Companion.TAB_HEIGHT
import com.ebicep.chatplus.features.chattabs.ChatTabs.createDefaultTab
import com.ebicep.chatplus.features.internal.Debug
import com.ebicep.chatplus.hud.ChatManager
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
class TabSettings {

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
            value.forEach { it.chatWindow = chatWindow }
            resetSortedChatTabs()
        }

    val selectedTab: ChatTab
        get() = tabs[selectedTabIndex]

    @Transient
    var sortedTabs: List<ChatTab> = listOf()

    @Transient
    lateinit var chatWindow: ChatWindow

    init {
        if (tabs.isEmpty()) {
            tabs.add(createDefaultTab())
        }
        selectedTabIndex = Mth.clamp(selectedTabIndex, 0, tabs.size - 1)

        tabs.forEach {
            it.regex = Regex(it.pattern)
        }
        resetSortedChatTabs()
    }

    fun clone(): TabSettings {
        return TabSettings().also {
            it.hideTabs = hideTabs
            it.showTabsWhenChatNotOpen = showTabsWhenChatNotOpen
            it.tabTextColorSelected = tabTextColorSelected
            it.tabTextColorUnselected = tabTextColorUnselected
            it.unfocusedTabOpacityMultiplier = unfocusedTabOpacityMultiplier
        }
    }

    override fun toString(): String {
        return "(${tabs.joinToString(",") { it.name }})"
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
        val translatedY = chatWindow.renderer.getUpdatedY() - y
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
        var xStart: Int = chatWindow.renderer.internalX
        val yStart: Int = chatWindow.renderer.internalY + CHAT_TAB_Y_OFFSET
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

                    if (!hideTabs && !chatWindow.generalSettings.disabled) {
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
        val isWindowSelected = chatWindow == ChatManager.selectedWindow
        val chatWindow = chatTab.chatWindow
        var backgroundColor = chatWindow.generalSettings.backgroundColor
        if (!isWindowSelected) {
            backgroundColor = reduceAlpha(backgroundColor, unfocusedTabOpacityMultiplier)
        }
        var textColor = if (isGlobalSelected) tabTextColorSelected else tabTextColorUnselected
        if (!isWindowSelected) {
            textColor = reduceAlpha(textColor, if (unfocusedTabOpacityMultiplier == 0f) .05f else chatWindow.generalSettings.unfocusedTextOpacityMultiplier)
        }
        val startY = if (isWindowSelected && chatTab == selectedTab) {
            if (chatWindow.outlineSettings.outlineTabType == OutlineSettings.OutlineTabType.SELECTED_TAB_OPEN_TOP) {
                -(chatTab.yStart - chatWindow.renderer.internalY)
            } else {
                -CHAT_TAB_Y_OFFSET
            }
        } else {
            0
        }

        poseStack.createPose {
            poseStack.guiForward(if (isGlobalSelected) 65.0 else 55.0)
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

}

data class ChatTabClickedEvent(
    val chatTab: ChatTab,
    val mouseX: Double,
    val mouseY: Double,
    val tabXStart: Double,
    val tabYStart: Double
)

data class ChatTabRenderEvent(
    val guiGraphics: GuiGraphics,
    val chatTab: ChatTab,
    val tabWidth: Int,
    var xStart: Int,
    var yStart: Int
)

data class ChatTabSwitchEvent(
    val oldTab: ChatTab,
    val newTab: ChatTab
)