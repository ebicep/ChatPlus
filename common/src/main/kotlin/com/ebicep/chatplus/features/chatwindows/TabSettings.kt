package com.ebicep.chatplus.features.chatwindows

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.EnumTranslatableName
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_X_SPACE
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_Y_OFFSET
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.ChatTab.Companion.TAB_HEIGHT
import com.ebicep.chatplus.features.chattabs.ChatTabSerializer
import com.ebicep.chatplus.features.chattabs.ChatTabs.createDefaultTab
import com.ebicep.chatplus.features.internal.Debug
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatManager.resetGlobalSortedTabs
import com.ebicep.chatplus.hud.ChatPlusScreen
import com.ebicep.chatplus.util.GraphicsUtil
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.drawImage
import com.ebicep.chatplus.util.GraphicsUtil.drawString0
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import com.ebicep.chatplus.util.KotlinUtil.reduceAlpha
import com.ebicep.chatplus.util.Resources
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import java.awt.Color
import kotlin.math.roundToInt

@Serializable
class TabSettings {

    var selectedTabIndex = 0
    var startRenderTabIndex = 0
    var hideTabs = false
    var showTabsWhenChatNotOpen: Boolean = false
    var position: Position = Position.BOTTOM
    var tabTextColorSelected: Int = Color(255, 255, 255, 255).rgb
    var tabTextColorUnselected: Int = Color(153, 153, 153, 255).rgb
    var unfocusedTabOpacityMultiplier: Float = .4f

    @Transient
    var tabPositionsInitialized: Boolean = false

    @Serializable(with = ChatTabSerializer::class)
    var tabs: MutableList<ChatTab> = mutableListOf()
        set(value) {
            field = if (value.isEmpty()) {
                val defaultTab = createDefaultTab()
                defaultTab.chatWindow = chatWindow
                mutableListOf(defaultTab)
            } else {
                value
            }
            selectedTabIndex = Mth.clamp(selectedTabIndex, 0, tabs.size - 1)
            selectedTab.unreadCount = 0
            field.forEach { it.chatWindow = chatWindow }
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
            it.updateServerSettings()
            it.updateCurrentSettings()
        }
        resetSortedChatTabs(false)
    }

    fun clone(): TabSettings {
        return TabSettings().also {
            it.hideTabs = hideTabs
            it.showTabsWhenChatNotOpen = showTabsWhenChatNotOpen
            it.position = position
            it.tabTextColorSelected = tabTextColorSelected
            it.tabTextColorUnselected = tabTextColorUnselected
            it.unfocusedTabOpacityMultiplier = unfocusedTabOpacityMultiplier
        }
    }

    override fun toString(): String {
        return "(${tabs.joinToString(",") { it.name }})"
    }

    fun resetSortedChatTabs(resetGlobal: Boolean = true) {
        sortedTabs = tabs.sortedBy { -it.priority }
        if (resetGlobal && Config.loaded) {
            resetGlobalSortedTabs()
        }
    }

    fun updateTabSettings(ip: String? = null) {
        tabs.forEach {
            it.updateCurrentSettings(ip ?: Minecraft.getInstance().player?.connection?.serverData?.ip)
        }
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
            if (tabs.first().xStart + totalWidth >= Minecraft.getInstance().window.guiScaledWidth) {
                startRenderTabIndex++
            }
        }
        if (Config.values.moveToTabWhenCycling) {
            switchToTab(tabs[Mth.clamp(selectedTabIndex + amount, 0, tabs.size - 1)])
        }
    }

    fun handleClickedTab(x: Double, y: Double) {
        val clickedTab: ChatTab = getClickedTab(x, y) ?: return
        EventBus.post(ChatTabClickedEvent(clickedTab, x, y, clickedTab.xStart.toDouble(), clickedTab.yStart.toDouble()))
        if (clickedTab != ChatManager.globalSelectedTab) {
            switchToTab(clickedTab)
        }
    }

    private fun switchToTab(newTab: ChatTab) {
        val oldTab = ChatManager.globalSelectedTab
        oldTab.resetFilter()
        selectedTabIndex = tabs.indexOf(newTab)
        queueUpdateConfig = true
        EventBus.post(ChatTabSwitchEvent(oldTab, newTab))
        val autoSend = newTab.autoSend
        if (autoSend.isNotEmpty()) {
            ChatPlusScreen.sendChatMessage(message = autoSend, addToSent = false)
        }
    }

    fun getClickedTab(x: Double, y: Double): ChatTab? {
        when (position) {
            Position.TOP -> {
                val translatedY = chatWindow.renderer.getUpdatedY() - y - chatWindow.renderer.getTotalLineHeight(true) - TAB_HEIGHT - CHAT_TAB_Y_OFFSET
                if (translatedY > CHAT_TAB_Y_OFFSET || translatedY < -TAB_HEIGHT + 2) {
                    return null
                }
            }

            Position.BOTTOM -> {
                val translatedY = chatWindow.renderer.getUpdatedY() - y
                if (translatedY > CHAT_TAB_Y_OFFSET - 4 || translatedY < -TAB_HEIGHT) {
                    return null
                }
            }
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
        val yStart: Int = when (position) {
            Position.TOP -> chatWindow.renderer.internalY - chatWindow.renderer.getTotalLineHeight(true).roundToInt() - TAB_HEIGHT - CHAT_TAB_Y_OFFSET
            Position.BOTTOM -> chatWindow.renderer.internalY + CHAT_TAB_Y_OFFSET
        }
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
                when (position) {
                    Position.TOP -> chatWindow.renderer.internalY - chatWindow.renderer.getTotalLineHeight(true).roundToInt() - chatTab.yStart // top of highest line
                    Position.BOTTOM -> -(chatTab.yStart - chatWindow.renderer.internalY)
                }
            } else {
                when (position) {
                    Position.TOP -> chatWindow.renderer.internalY - chatWindow.renderer.getTotalLineHeight(true).roundToInt() - chatTab.yStart - CHAT_TAB_Y_OFFSET
                    Position.BOTTOM -> -CHAT_TAB_Y_OFFSET
                }
            }
        } else {
            when (position) {
                Position.TOP -> TAB_HEIGHT // start from bottom
                Position.BOTTOM -> 0
            }
        }

        poseStack.createPose {
            poseStack.guiForward(GraphicsUtil.GuiForwardType.ChatWindowTab) { isGlobalSelected }
            guiGraphics.fill(
                0,
                startY,
                chatTab.width,
                when (position) {
                    Position.TOP -> 0 // fill to top
                    Position.BOTTOM -> TAB_HEIGHT
                },
                backgroundColor
            )
            poseStack.guiForward()
            guiGraphics.drawString0(
                chatTab.name,
                ChatTab.PADDING,
                ChatTab.PADDING + ChatTab.PADDING / 2,
                textColor
            )
        }
        // notification badge
        if (Config.values.tabNotificationSettings.enabled && chatTab.unreadCount > 0 && !chatTab.notificationSettings.disableNotifications) {
            val scale = Config.values.tabNotificationSettings.scale
            poseStack.createPose {
                poseStack.guiForward(GraphicsUtil.GuiForwardType.ChatTabNotificationBadge)
                poseStack.translate0(
                    x = chatTab.width - Resources.NOTIFICATION_BADGE.width / 2 * scale,
                    y = startY - Resources.NOTIFICATION_BADGE.height / 2 * scale - if (position == Position.TOP) TAB_HEIGHT else 0
                )
                poseStack.scale(scale, scale, 1f)
                guiGraphics.drawImage(Resources.NOTIFICATION_BADGE)
                if (Config.values.tabNotificationSettings.showCount) {
                    poseStack.guiForward()
                    guiGraphics.drawString0(
                        chatTab.unreadCount.toString(),
                        Resources.NOTIFICATION_BADGE.width / 2 - Minecraft.getInstance().font.width(chatTab.unreadCount.toString()) / 2,
                        Resources.NOTIFICATION_BADGE.height / 2 - Minecraft.getInstance().font.lineHeight / 2,
                        Config.values.tabNotificationSettings.countColor
                    )
                }
            }
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

    fun cloneTab(chatTab: ChatTab) {
        val mutableList = tabs.toMutableList()
        mutableList.add(chatTab.clone())
        tabs = mutableList
        updateTabSettings()
        resetGlobalSortedTabs()
    }

    fun removeTab(chatTab: ChatTab) {
        if (!tabs.contains(chatTab)) {
            return
        }
        val mutableList = tabs.toMutableList()
        mutableList.remove(chatTab)
        tabs = mutableList
    }

    @Serializable
    enum class Position(key: String) : EnumTranslatableName {
        TOP("chatPlus.chatWindow.tabSettings.position.top"),
        BOTTOM("chatPlus.chatWindow.tabSettings.position.bottom"),

        ;

        val translatable: Component = Component.translatable(key)

        override fun getTranslatableName(): Component {
            return translatable
        }
    }

}

data class ChatTabClickedEvent(
    val chatTab: ChatTab,
    val mouseX: Double,
    val mouseY: Double,
    val tabXStart: Double,
    val tabYStart: Double,
)

data class ChatTabRenderEvent(
    val guiGraphics: GuiGraphics,
    val chatTab: ChatTab,
    val tabWidth: Int,
    var xStart: Int,
    var yStart: Int,
)

data class ChatTabSwitchEvent(
    val oldTab: ChatTab,
    val newTab: ChatTab,
)