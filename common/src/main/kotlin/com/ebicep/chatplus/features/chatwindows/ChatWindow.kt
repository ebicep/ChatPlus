package com.ebicep.chatplus.features.chatwindows

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.features.chattabs.AutoTabCreator
import com.ebicep.chatplus.hud.ChatRenderer
import kotlinx.serialization.Serializable

@Serializable
class ChatWindow {

    var generalSettings: GeneralSettings = GeneralSettings()
    var outlineSettings: OutlineSettings = OutlineSettings()
    var padding: Padding = Padding()
    val renderer = ChatRenderer()
    var tabSettings: TabSettings = TabSettings()
    var autoTabCreator: AutoTabCreator = AutoTabCreator()

    init {
        ChatPlus.LOGGER.info("Create $this")
        generalSettings.chatWindow = this
        tabSettings.chatWindow = this
        tabSettings.tabs.forEach {
            it.chatWindow = this
        }
        renderer.chatWindow = this
        autoTabCreator.chatWindow = this
    }

    /**
     * Returns a clone ChatWindow matching the visual properties of this ChatWindow. Excluding renderer and tabs.
     */
    fun clone(): ChatWindow {
        return ChatWindow().also {
            it.generalSettings = generalSettings.clone().also { setting -> setting.chatWindow = it }
            it.outlineSettings = outlineSettings.clone()
            it.padding = padding.clone()
            it.tabSettings = tabSettings.clone().also { setting -> setting.chatWindow = it }
        }
    }

    override fun toString(): String {
        return "ChatWindow$tabSettings"
    }

}