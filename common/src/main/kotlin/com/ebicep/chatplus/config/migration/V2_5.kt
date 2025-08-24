@file:UseSerializers(
    KeySerializer::class
)

package com.ebicep.chatplus.config.migration

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.MOD_ID
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.serializers.KeySerializer
import com.ebicep.chatplus.features.chattabs.AutoTabCreator
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.ServerChatTabSettings
import com.ebicep.chatplus.features.chatwindows.*
import com.ebicep.chatplus.features.chatwindows.TabSettings.Position
import com.ebicep.chatplus.features.internal.MessageFilter
import com.ebicep.chatplus.features.internal.MessageFilterFormatted
import com.ebicep.chatplus.hud.ChatRenderer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.awt.Color


object V2_5 : Migrator<SchemaV2_5> {

    override fun getFileNameVersion(): String {
        return "$MOD_ID-v2.5.0"
    }

    override fun getSerializer(): KSerializer<SchemaV2_5> {
        return SchemaV2_5.serializer()
    }

    override fun migrate(old: SchemaV2_5) {
        migrateChatWindows(old)
    }

    fun migrateChatWindows(old: SchemaV2_5) {
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
                        oldChatTab.serverTabPatterns.forEach {
                            chatTabSettings.add(
                                ServerChatTabSettings(
                                    it.chatPattern.pattern,
                                    false,
                                ).also { settings ->
                                    settings.serverPattern = MessageFilter(it.pattern)
                                    settings.name = mainTabSettings.name
                                    settings.autoPrefix = mainTabSettings.autoPrefix
                                    settings.priority = mainTabSettings.priority
                                    settings.alwaysAdd = mainTabSettings.alwaysAdd
                                    settings.skipOthers = mainTabSettings.skipOthers
                                    settings.commandsOverrideAutoPrefix = mainTabSettings.commandsOverrideAutoPrefix
                                })
                        }
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
data class SchemaV2_5(
    var chatWindows: MutableList<SchemaV2_5_ChatWindow> = mutableListOf(),
)

@Serializable
class SchemaV2_5_ChatWindow {
    var tabSettings: SchemaV2_5_TabSettings = SchemaV2_5_TabSettings()
    var generalSettings: GeneralSettings = GeneralSettings()
    var outlineSettings: OutlineSettings = OutlineSettings()
    var padding: Padding = Padding()
    val renderer = ChatRenderer()
    var autoTabCreator: AutoTabCreator = AutoTabCreator()
}

@Serializable
class SchemaV2_5_TabSettings {
    var tabs: MutableList<SchemaV2_5_ChatTab> = mutableListOf()
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
class SchemaV2_5_ChatTab {
    var pattern: String = ""
    var formatted: Boolean = false
    var name: String = ""
    var autoPrefix: String = ""
    var serverTabPatterns = mutableListOf<SchemaV2_5_ServerTabPattern>()
    var priority: Int = 0
    var alwaysAdd: Boolean = false
    var skipOthers: Boolean = false
    var commandsOverrideAutoPrefix: Boolean = true
}

@Serializable
class SchemaV2_5_ServerTabPattern : MessageFilter {

    var chatPattern = MessageFilterFormatted("", false)
    var autoPrefix: String = ""

    constructor(pattern: String, autoPrefix: String) : super(pattern) {
        this.autoPrefix = autoPrefix
    }

}
