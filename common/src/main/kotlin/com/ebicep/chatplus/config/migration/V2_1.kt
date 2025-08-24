@file:UseSerializers(
    KeySerializer::class
)

package com.ebicep.chatplus.config.migration

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.MOD_ID
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.serializers.KeySerializer
import com.ebicep.chatplus.config.serializers.KeyWithModifier
import com.ebicep.chatplus.features.chattabs.AutoTabCreator
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.ServerChatTabSettings
import com.ebicep.chatplus.features.chatwindows.*
import com.ebicep.chatplus.features.chatwindows.TabSettings.Position
import com.ebicep.chatplus.hud.ChatRenderer
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.awt.Color


object V2_1 : Migrator<SchemaV2_1> {

    override fun getFileNameVersion(): String {
        return "$MOD_ID-v2.1.0"
    }

    override fun getSerializer(): KSerializer<SchemaV2_1> {
        return SchemaV2_1.serializer()
    }

    override fun migrate(old: SchemaV2_1) {
        val values = Config.values
        values.movableChatKey = KeyWithModifier(old.movableChatToggleKey, 0)
        migrateChatWindows(old)
    }

    fun migrateChatWindows(old: SchemaV2_1) {
        val newChatWindows = mutableListOf<ChatWindow>()
        old.chatWindows.forEach { oldChatWindow ->
            ChatPlus.LOGGER.info("Migrating chat window: $oldChatWindow")
            val newChatWindow = ChatWindow().also { window ->
                window.generalSettings = oldChatWindow.generalSettings
                window.outlineSettings = oldChatWindow.outlineSettings
                window.padding = oldChatWindow.padding
                window.renderer = oldChatWindow.renderer
                window.autoTabCreator = oldChatWindow.autoTabCreator
                window.tabSettings = TabSettings().also { settings ->
                    settings.selectedTabIndex = oldChatWindow.tabSettings.selectedTabIndex
                    settings.startRenderTabIndex = oldChatWindow.tabSettings.startRenderTabIndex
                    settings.hideTabs = oldChatWindow.tabSettings.hideTabs
                    settings.showTabsWhenChatNotOpen = oldChatWindow.tabSettings.showTabsWhenChatNotOpen
                    settings.position = oldChatWindow.tabSettings.position
                    settings.tabTextColorSelected = oldChatWindow.tabSettings.tabTextColorSelected
                    settings.tabTextColorUnselected = oldChatWindow.tabSettings.tabTextColorUnselected
                    settings.unfocusedTabOpacityMultiplier = oldChatWindow.tabSettings.unfocusedTabOpacityMultiplier
                    val newChatTabs = mutableListOf<ChatTab>()
                    oldChatWindow.tabSettings.tabs.forEach { oldChatTab ->
                        val chatTabSettings = mutableListOf<ServerChatTabSettings>()
                        val mainTabSettings = ServerChatTabSettings(
                            oldChatTab.pattern,
                            oldChatTab.formatted,
                        ).also { settings ->
                            settings.name = oldChatTab.name
                            settings.autoPrefix = oldChatTab.autoPrefix
                            settings.priority = oldChatTab.priority
                            settings.alwaysAdd = oldChatTab.alwaysAdd
                            settings.skipOthers = oldChatTab.skipOthers
                            settings.commandsOverrideAutoPrefix = oldChatTab.commandsOverrideAutoPrefix
                        }
                        chatTabSettings.add(mainTabSettings)
                        newChatTabs.add(ChatTab(chatTabSettings).also { chatTab ->
                            chatTab.settings.forEach {
                                it.chatTab = chatTab
                            }
                        })
                        ChatPlus.LOGGER.info("Migrated chat tabs: $mainTabSettings - $newChatTabs")
                    }
                    settings.tabs.clear()
                    settings.tabs.addAll(newChatTabs)
                }
            }
            newChatWindow.updateWindowReference()
            newChatWindows.add(newChatWindow)
            ChatPlus.LOGGER.info("Done migrating chat window: $oldChatWindow")
        }
        Config.values.chatWindows = newChatWindows
    }

}

@Serializable
data class SchemaV2_1(

    var movableChatToggleKey: InputConstants.Key = InputConstants.getKey("key.keyboard.right.control"),
    var chatWindows: MutableList<SchemaV2_1_ChatWindow> = mutableListOf(),

    )

@Serializable
class SchemaV2_1_ChatWindow {
    var tabSettings: SchemaV2_1_TabSettings = SchemaV2_1_TabSettings()
    var generalSettings: GeneralSettings = GeneralSettings()
    var outlineSettings: OutlineSettings = OutlineSettings()
    var padding: Padding = Padding()
    val renderer = ChatRenderer()
    var autoTabCreator: AutoTabCreator = AutoTabCreator()
}

@Serializable
class SchemaV2_1_TabSettings {
    var tabs: MutableList<SchemaV2_1_ChatTab> = mutableListOf()
    var selectedTabIndex = 0
    var startRenderTabIndex = 0
    var hideTabs = false
    var showTabsWhenChatNotOpen: Boolean = false
    var position: Position = Position.BOTTOM
    var tabTextColorSelected: Int = Color(255, 255, 255, 255).rgb
    var tabTextColorUnselected: Int = Color(153, 153, 153, 255).rgb
    var unfocusedTabOpacityMultiplier: Float = .4f

}

@Serializable
class SchemaV2_1_ChatTab {
    var pattern: String = ""
    var formatted: Boolean = false
    var name: String = ""
    var autoPrefix: String = ""
    var priority: Int = 0
    var alwaysAdd: Boolean = false
    var skipOthers: Boolean = false
    var commandsOverrideAutoPrefix: Boolean = true
}
