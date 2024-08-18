package com.ebicep.chatplus.features.chatwindows

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.queueUpdateConfig
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.*
import com.ebicep.chatplus.features.chattabs.ChatTabs.DefaultTab
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderer
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth
import java.awt.Color

@Serializable
class ChatWindow {

    var backgroundColor: Int = Color(0f, 0f, 0f, .5f).rgb
    var outline: Boolean = false
    var outlineColor: Int = Color(0f, 0f, 0f, 0f).rgb
    val renderer = ChatRenderer()
    var hideTabs = false
    var selectedTabIndex = 0
    private var startRenderTabIndex = 0
    var tabs: MutableList<ChatTab> = mutableListOf()

    val selectedTab: ChatTab
        get() = tabs[selectedTabIndex]

    @Transient
    var sortedTabs: List<ChatTab> = listOf()

    init {
        ChatPlus.LOGGER.info("ChatWindow init")
        // correct values
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
        val translatedY = renderer.getUpdatedY() - y
        if (translatedY > CHAT_TAB_Y_OFFSET - 4 || translatedY < -(9 + ChatTab.PADDING + ChatTab.PADDING)) {
            return
        }
        tabs.forEachIndexed { index, it ->
            if (index < startRenderTabIndex) {
                return@forEachIndexed
            }
            val insideTabX = it.xStart < x && x < it.xEnd
            if (insideTabX) {
                EventBus.post(ChatTabClickedEvent(it, x, it.xStart))
                if (it != ChatManager.globalSelectedTab) {
                    val oldTab = ChatManager.globalSelectedTab
                    oldTab.resetFilter()
                    selectedTabIndex = index
                    queueUpdateConfig = true
                    ChatManager.globalSelectedTab.queueRefreshDisplayedMessages(false)
                    EventBus.post(ChatTabSwitchEvent(oldTab, it))
                    return
                }
            }
        }
    }

    fun renderTabs(guiGraphics: GuiGraphics) {
        if (hideTabs) {
            return
        }
        val poseStack = guiGraphics.pose()
        var xStart = renderer.internalX.toDouble()
        val yStart = renderer.internalY.toDouble() + CHAT_TAB_Y_OFFSET
        poseStack.createPose {
            poseStack.translate0(y = yStart)
            tabs.forEachIndexed { index, it ->
                if (index < startRenderTabIndex) {
                    return@forEachIndexed
                }
                poseStack.createPose {
                    val tabWidth = it.width

                    val translateX = EventBus.post(ChatTabRenderEvent(poseStack, it, tabWidth, xStart)).xStart
                    poseStack.translate0(x = translateX)

                    it.xStart = translateX
                    it.y = yStart

                    renderTab(it, guiGraphics)

                    xStart += tabWidth + CHAT_TAB_X_SPACE
                }
            }
        }
    }

    private fun renderTab(chatTab: ChatTab, guiGraphics: GuiGraphics) {
        val poseStack = guiGraphics.pose()
        val isSelected = chatTab == ChatManager.globalSelectedTab
        val backgroundOpacity = ((if (isSelected) 255 else 100) * ChatManager.getBackgroundOpacity()).toInt() shl 24
        val textColor = if (isSelected) 0xffffff else 0x999999

        poseStack.createPose {
            poseStack.guiForward()
            guiGraphics.fill(
                0,
                0,
                chatTab.width,
                9 + ChatTab.PADDING + ChatTab.PADDING,
                backgroundOpacity
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


}