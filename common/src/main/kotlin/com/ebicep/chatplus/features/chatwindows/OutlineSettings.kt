package com.ebicep.chatplus.features.chatwindows

import com.ebicep.chatplus.config.EnumTranslatableName
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_HEIGHT
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.ChatTab.Companion.TAB_HEIGHT
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderer
import com.ebicep.chatplus.util.GraphicsUtil.drawHorizontalLine
import com.ebicep.chatplus.util.GraphicsUtil.renderOutlineSetPos
import com.ebicep.chatplus.util.KotlinUtil.reduceAlpha
import kotlinx.serialization.Serializable
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import java.awt.Color
import kotlin.math.roundToInt

private const val THICKNESS = 1

@Serializable
class OutlineSettings {

    var enabled: Boolean = false
    var showWhenChatNotOpen: Boolean = false
    var outlineColor: Int = Color(0f, 0f, 0f, 0f).rgb
    var unfocusedOutlineColorOpacityMultiplier: Float = .4f
    var reduceUnfocusedOutlineOpacityWhenClosed: Boolean = false
    var outlineBoxType: OutlineBoxType = OutlineBoxType.TEXT_BOX
    var outlineTabType: OutlineTabType = OutlineTabType.SELECTED_TAB_OPEN_TOP

    fun clone(): OutlineSettings {
        return OutlineSettings().also {
            it.enabled = enabled
            it.outlineColor = outlineColor
            it.unfocusedOutlineColorOpacityMultiplier = unfocusedOutlineColorOpacityMultiplier
            it.reduceUnfocusedOutlineOpacityWhenClosed = reduceUnfocusedOutlineOpacityWhenClosed
            it.outlineBoxType = outlineBoxType
            it.outlineTabType = outlineTabType
        }
    }

    fun getUpdatedOutlineColor(chatWindow: ChatWindow): Int {
        val isFocused = chatWindow == ChatManager.selectedWindow
        val applyReductionWhenClosed = reduceUnfocusedOutlineOpacityWhenClosed && !ChatManager.isChatFocused()
        if (isFocused && !applyReductionWhenClosed) {
            return outlineColor
        }
        return reduceAlpha(outlineColor, unfocusedOutlineColorOpacityMultiplier)
    }

    enum class OutlineBoxType(key: String) : EnumTranslatableName {

        NONE("chatPlus.chatWindow.outlineSettings.outlineBoxType.none"),
        WHOLE_BOX("chatPlus.chatWindow.outlineSettings.outlineBoxType.wholeBox") {
            override fun render(outlineTabType: OutlineTabType, guiGraphics: GuiGraphics, chatWindow: ChatWindow, selectedTab: ChatTab, renderer: ChatRenderer) {
                val position = chatWindow.tabSettings.position
                val h = renderer.getTotalLineHeight(true)
                guiGraphics.renderOutlineSetPos(
                    renderer.internalX.toFloat() - THICKNESS,
                    renderer.internalY.toFloat() - h - (if (!chatWindow.tabSettings.hideTabs && position == TabSettings.Position.TOP) CHAT_TAB_HEIGHT else THICKNESS),
                    renderer.internalX.toFloat() + renderer.internalWidth.toFloat() + THICKNESS,
                    renderer.internalY.toFloat() + (if (!chatWindow.tabSettings.hideTabs && position == TabSettings.Position.BOTTOM) CHAT_TAB_HEIGHT else THICKNESS),
                    chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                    THICKNESS.toFloat(),
                )
            }
        },
        TEXT_AND_TAB_BOX("chatPlus.chatWindow.outlineSettings.outlineBoxType.textAndTabBox") {
            override fun render(outlineTabType: OutlineTabType, guiGraphics: GuiGraphics, chatWindow: ChatWindow, selectedTab: ChatTab, renderer: ChatRenderer) {
                WHOLE_BOX.render(outlineTabType, guiGraphics, chatWindow, selectedTab, renderer)
                if (chatWindow.tabSettings.hideTabs) {
                    return
                }
                // render top tab box line
                val position = chatWindow.tabSettings.position
                val y = when (position) {
                    TabSettings.Position.TOP -> renderer.internalY - renderer.getTotalLineHeight(true).roundToInt() - THICKNESS
                    TabSettings.Position.BOTTOM -> renderer.internalY
                }
                if (outlineTabType == OutlineTabType.SELECTED_TAB_OPEN_TOP) {
                    guiGraphics.drawHorizontalLine(
                        renderer.internalX,
                        selectedTab.xStart - THICKNESS,
                        y,
                        chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                        THICKNESS
                    )
                    guiGraphics.drawHorizontalLine(
                        selectedTab.xEnd + THICKNESS,
                        renderer.backgroundWidthEndX,
                        y,
                        chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                        THICKNESS
                    )
                } else {
                    guiGraphics.drawHorizontalLine(
                        renderer.internalX,
                        renderer.backgroundWidthEndX,
                        y,
                        chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                        THICKNESS
                    )
                }
            }
        },
        TEXT_BOX("chatPlus.chatWindow.outlineSettings.outlineBoxType.textBox") {
            override fun render(outlineTabType: OutlineTabType, guiGraphics: GuiGraphics, chatWindow: ChatWindow, selectedTab: ChatTab, renderer: ChatRenderer) {
                val position = chatWindow.tabSettings.position
                val h = renderer.getTotalLineHeight(true)
                val top = chatWindow.tabSettings.hideTabs || outlineTabType != OutlineTabType.SELECTED_TAB_OPEN_TOP || position != TabSettings.Position.TOP
                val bottom = chatWindow.tabSettings.hideTabs || outlineTabType != OutlineTabType.SELECTED_TAB_OPEN_TOP || position != TabSettings.Position.BOTTOM
                guiGraphics.renderOutlineSetPos(
                    renderer.internalX.toFloat() - THICKNESS,
                    renderer.internalY.toFloat() - h - THICKNESS - (if (!top) THICKNESS else 0),
                    renderer.internalX.toFloat() + renderer.internalWidth.toFloat() + THICKNESS,
                    renderer.internalY.toFloat() + THICKNESS + (if (!bottom) THICKNESS else 0),
                    chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                    THICKNESS.toFloat(),
                    top = top,
                    bottom = bottom,
                )
                if (chatWindow.tabSettings.hideTabs) {
                    return
                }
                if (outlineTabType != OutlineTabType.SELECTED_TAB_OPEN_TOP) {
                    return
                }
                val y = when (position) {
                    TabSettings.Position.TOP -> renderer.internalY - renderer.getTotalLineHeight(true).roundToInt() - THICKNESS
                    TabSettings.Position.BOTTOM -> renderer.internalY
                }
                guiGraphics.drawHorizontalLine(
                    renderer.internalX,
                    selectedTab.xStart,
                    y,
                    chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                    THICKNESS
                )
                guiGraphics.drawHorizontalLine(
                    selectedTab.xEnd,
                    renderer.backgroundWidthEndX,
                    y,
                    chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                    THICKNESS
                )
            }
        },
        TAB_BOX("chatPlus.chatWindow.outlineSettings.outlineBoxType.tabBox") {
            override fun render(outlineTabType: OutlineTabType, guiGraphics: GuiGraphics, chatWindow: ChatWindow, selectedTab: ChatTab, renderer: ChatRenderer) {
                if (chatWindow.tabSettings.hideTabs) {
                    return
                }
                val position = chatWindow.tabSettings.position
                val startY = when (position) {
                    TabSettings.Position.TOP -> renderer.internalY - renderer.getTotalLineHeight(true) - CHAT_TAB_HEIGHT
                    TabSettings.Position.BOTTOM -> renderer.internalY.toFloat()
                }
                val endY = when (position) {
                    TabSettings.Position.TOP -> renderer.internalY - renderer.getTotalLineHeight(true)
                    TabSettings.Position.BOTTOM -> renderer.internalY.toFloat() + CHAT_TAB_HEIGHT
                }
                guiGraphics.renderOutlineSetPos(
                    renderer.internalX.toFloat() - THICKNESS,
                    startY,
                    renderer.internalX.toFloat() + renderer.internalWidth.toFloat() + THICKNESS,
                    endY,
                    chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                    THICKNESS.toFloat(),
                    top = outlineTabType != OutlineTabType.SELECTED_TAB_OPEN_TOP || position != TabSettings.Position.TOP,
                    bottom = outlineTabType != OutlineTabType.SELECTED_TAB_OPEN_TOP || position != TabSettings.Position.BOTTOM
                )
                if (outlineTabType != OutlineTabType.SELECTED_TAB_OPEN_TOP) {
                    return
                }
                val y = when (position) {
                    TabSettings.Position.TOP -> renderer.internalY - renderer.getTotalLineHeight(true).roundToInt() - CHAT_TAB_HEIGHT
                    TabSettings.Position.BOTTOM -> renderer.internalY
                }
                guiGraphics.drawHorizontalLine(
                    renderer.internalX - THICKNESS,
                    selectedTab.xStart - THICKNESS,
                    y,
                    chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                    THICKNESS
                )
                guiGraphics.drawHorizontalLine(
                    selectedTab.xEnd + THICKNESS,
                    renderer.backgroundWidthEndX + THICKNESS,
                    y,
                    chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                    THICKNESS
                )
            }
        },

        ;

        val translatable: Component = Component.translatable(key)

        open fun render(outlineTabType: OutlineTabType, guiGraphics: GuiGraphics, chatWindow: ChatWindow, selectedTab: ChatTab, renderer: ChatRenderer) {

        }

        override fun getTranslatableName(): Component {
            return translatable
        }

    }

    enum class OutlineTabType(key: String) : EnumTranslatableName {

        NONE("chatPlus.chatWindow.outlineSettings.outlineTabType.none"),
        SELECTED_TAB("chatPlus.chatWindow.outlineSettings.outlineTabType.selectedTab") {
            override fun render(outlineBoxType: OutlineBoxType, guiGraphics: GuiGraphics, chatWindow: ChatWindow, selectedTab: ChatTab, renderer: ChatRenderer) {
                guiGraphics.renderOutlineSetPos(
                    selectedTab.xStart - THICKNESS,
                    selectedTab.yStart - THICKNESS,
                    selectedTab.xEnd + THICKNESS,
                    selectedTab.yStart + TAB_HEIGHT + THICKNESS,
                    chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                    THICKNESS
                )
            }
        },
        SELECTED_TAB_OPEN_TOP("chatPlus.chatWindow.outlineSettings.outlineTabType.selectedTabOpenTop") {
            override fun render(outlineBoxType: OutlineBoxType, guiGraphics: GuiGraphics, chatWindow: ChatWindow, selectedTab: ChatTab, renderer: ChatRenderer) {
                when (chatWindow.tabSettings.position) {
                    TabSettings.Position.TOP -> {
                        guiGraphics.renderOutlineSetPos(
                            selectedTab.xStart - THICKNESS,
                            selectedTab.yStart - THICKNESS,
                            selectedTab.xEnd + THICKNESS,
                            selectedTab.yStart + TAB_HEIGHT + THICKNESS + THICKNESS,
                            chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                            THICKNESS,
                            bottom = false
                        )
                    }

                    TabSettings.Position.BOTTOM -> {
                        guiGraphics.renderOutlineSetPos(
                            selectedTab.xStart - THICKNESS,
                            renderer.internalY - THICKNESS,
                            selectedTab.xEnd + THICKNESS,
                            selectedTab.yStart + TAB_HEIGHT + THICKNESS,
                            chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                            THICKNESS,
                            top = false
                        )
                    }
                }
            }
        },
        EVERY_TAB("chatPlus.chatWindow.outlineSettings.outlineTabType.everyTab") {
            override fun render(outlineBoxType: OutlineBoxType, guiGraphics: GuiGraphics, chatWindow: ChatWindow, selectedTab: ChatTab, renderer: ChatRenderer) {
                chatWindow.tabSettings.tabs.forEach { SELECTED_TAB.render(outlineBoxType, guiGraphics, chatWindow, it, renderer) }
            }
        },

        ;

        val translatable: Component = Component.translatable(key)

        open fun render(outlineBoxType: OutlineBoxType, guiGraphics: GuiGraphics, chatWindow: ChatWindow, selectedTab: ChatTab, renderer: ChatRenderer) {

        }

        override fun getTranslatableName(): Component {
            return translatable
        }

    }
}