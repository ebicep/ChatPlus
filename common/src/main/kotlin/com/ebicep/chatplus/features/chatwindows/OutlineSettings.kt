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

private const val THICKNESS = 1

@Serializable
class OutlineSettings {

    var enabled: Boolean = false
    var showWhenChatNotOpen: Boolean = false
    var outlineColor: Int = Color(0f, 0f, 0f, 0f).rgb
    var unfocusedOutlineColorOpacityMultiplier: Float = .4f
    var outlineBoxType: OutlineBoxType = OutlineBoxType.TEXT_BOX
    var outlineTabType: OutlineTabType = OutlineTabType.SELECTED_TAB_OPEN_TOP

    fun clone(): OutlineSettings {
        return OutlineSettings().also {
            it.enabled = enabled
            it.outlineColor = outlineColor
            it.unfocusedOutlineColorOpacityMultiplier = unfocusedOutlineColorOpacityMultiplier
            it.outlineBoxType = outlineBoxType
            it.outlineTabType = outlineTabType
        }
    }

    fun getUpdatedOutlineColor(chatWindow: ChatWindow): Int {
        if (chatWindow == ChatManager.selectedWindow) {
            return outlineColor
        }
        return reduceAlpha(outlineColor, unfocusedOutlineColorOpacityMultiplier)
    }

    enum class OutlineBoxType(key: String) : EnumTranslatableName {

        NONE("chatPlus.chatWindow.outlineSettings.outlineBoxType.none"),
        WHOLE_BOX("chatPlus.chatWindow.outlineSettings.outlineBoxType.wholeBox") {
            override fun render(outlineTabType: OutlineTabType, guiGraphics: GuiGraphics, chatWindow: ChatWindow, selectedTab: ChatTab, renderer: ChatRenderer) {
                val h = renderer.getTotalLineHeight()
                guiGraphics.renderOutlineSetPos(
                    renderer.internalX.toFloat() - THICKNESS,
                    renderer.internalY.toFloat() - h - THICKNESS,
                    renderer.internalX.toFloat() + renderer.internalWidth.toFloat() + THICKNESS,
                    renderer.internalY.toFloat() + (if (chatWindow.tabSettings.hideTabs) 0 else CHAT_TAB_HEIGHT),
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
                if (outlineTabType == OutlineTabType.SELECTED_TAB_OPEN_TOP) {
                    guiGraphics.drawHorizontalLine(
                        renderer.internalX,
                        selectedTab.xStart - THICKNESS,
                        renderer.internalY,
                        chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                        THICKNESS
                    )
                    guiGraphics.drawHorizontalLine(
                        selectedTab.xEnd + THICKNESS,
                        renderer.backgroundWidthEndX,
                        renderer.internalY,
                        chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                        THICKNESS
                    )
                } else {
                    guiGraphics.drawHorizontalLine(
                        renderer.internalX,
                        renderer.backgroundWidthEndX,
                        renderer.internalY,
                        chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                        THICKNESS
                    )
                }
            }
        },
        TEXT_BOX("chatPlus.chatWindow.outlineSettings.outlineBoxType.textBox") {
            override fun render(outlineTabType: OutlineTabType, guiGraphics: GuiGraphics, chatWindow: ChatWindow, selectedTab: ChatTab, renderer: ChatRenderer) {
                val h = renderer.getTotalLineHeight()
                guiGraphics.renderOutlineSetPos(
                    renderer.internalX.toFloat() - THICKNESS,
                    renderer.internalY.toFloat() - h - THICKNESS,
                    renderer.internalX.toFloat() + renderer.internalWidth.toFloat() + THICKNESS,
                    renderer.internalY.toFloat() + THICKNESS,
                    chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                    THICKNESS.toFloat(),
                    bottom = chatWindow.tabSettings.hideTabs || outlineTabType != OutlineTabType.SELECTED_TAB_OPEN_TOP
                )
                if (chatWindow.tabSettings.hideTabs) {
                    return
                }
                if (outlineTabType == OutlineTabType.SELECTED_TAB_OPEN_TOP) {
                    guiGraphics.drawHorizontalLine(
                        renderer.internalX,
                        selectedTab.xStart,
                        renderer.internalY,
                        chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                        THICKNESS
                    )
                    guiGraphics.drawHorizontalLine(
                        selectedTab.xEnd,
                        renderer.backgroundWidthEndX,
                        renderer.internalY,
                        chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                        THICKNESS
                    )
                }
            }
        },
        TAB_BOX("chatPlus.chatWindow.outlineSettings.outlineBoxType.tabBox") {
            override fun render(outlineTabType: OutlineTabType, guiGraphics: GuiGraphics, chatWindow: ChatWindow, selectedTab: ChatTab, renderer: ChatRenderer) {
                if (chatWindow.tabSettings.hideTabs) {
                    return
                }
                guiGraphics.renderOutlineSetPos(
                    renderer.internalX.toFloat() - THICKNESS,
                    renderer.internalY.toFloat(),
                    renderer.internalX.toFloat() + renderer.internalWidth.toFloat() + THICKNESS,
                    renderer.internalY.toFloat() + CHAT_TAB_HEIGHT,
                    chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                    THICKNESS.toFloat(),
                    top = outlineTabType != OutlineTabType.SELECTED_TAB_OPEN_TOP
                )
                if (outlineTabType == OutlineTabType.SELECTED_TAB_OPEN_TOP) {
                    guiGraphics.drawHorizontalLine(
                        renderer.internalX,
                        selectedTab.xStart - THICKNESS,
                        renderer.internalY,
                        chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                        THICKNESS
                    )
                    guiGraphics.drawHorizontalLine(
                        selectedTab.xEnd + THICKNESS,
                        renderer.backgroundWidthEndX,
                        renderer.internalY,
                        chatWindow.outlineSettings.getUpdatedOutlineColor(chatWindow),
                        THICKNESS
                    )
                }
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