package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.features.internal.MessageFilter
import com.ebicep.chatplus.features.internal.MessageFilterFormatted

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

@Serializable
class AutoTabCreator {

    var autoTabOptions: MutableList<AutoTabOptions> = mutableListOf()

    @Transient
    lateinit var chatWindow: ChatWindow

    init {
        autoTabOptions.forEach {
            it.updateRegex()
        }
        EventBus.register<AddNewMessageEvent>({ -5 }) {
            handleNewMessage(it.rawComponent)?.let { data ->
                data.chatTab.addNewMessage(it)
                if (data.autoTabOptions.skipOthersOnCreation) {
                    it.returnFunction = true
                }
            }
        }
        EventBus.register<SkipNewMessageEvent> {
            handleNewMessage(it.rawComponent)?.chatTab?.addNewMessage(
                AddNewMessageEvent(
                    it.mutableComponent,
                    it.rawComponent,
                    it.senderUUID,
                    it.signature,
                    it.addedTime,
                    it.tag,
                )
            )
        }
    }

    private fun handleNewMessage(component: Component): AutoTabData? {
        if (autoTabOptions.isEmpty()) {
            return null
        }
        val text = component.string
        val tabSettings = chatWindow.tabSettings
        // check if already in other tab
        if (tabSettings.tabs.any { it.isAutoTab && it.currentSettings.matches(component) }) {
            return null
        }
        autoTabOptions.forEach {
            val name = Minecraft.getInstance().player?.gameProfile?.name ?: ""
            val filterFormatted = MessageFilterFormatted(it.pattern.replace("%PLAYER%", name, ignoreCase = true), it.formatted)
            // check matches and get group index
            val matchResult: MatchResult = filterFormatted.find(text) ?: return@forEach
            val tabName = formatRegex(it.tabNameFormatter, matchResult)
            val pattern = formatRegex(it.regexFormatter, matchResult)
            val autoSend = formatRegex(it.autoSendFormatter, matchResult)
            val autoPrefix = formatRegex(it.autoPrefixFormatter, matchResult)
            val chatTab = ChatTab(
                chatWindow,
                ServerChatTabSettings(pattern, it.formatted).also { settings ->
                    settings.name = tabName
                    settings.autoSend = autoSend
                    settings.autoPrefix = autoPrefix
                    settings.priority = it.priority
                    settings.alwaysAdd = it.alwaysAdd
                    settings.skipOthers = it.skipOthers
                    settings.commandsOverrideAutoPrefix = it.commandsOverrideAutoPrefix
                    settings.suggestionsPatterns = it.suggestionsPatterns.map {
                        ServerChatTabCommandSuggestion(
                            MessageFilter(formatRegex(it.commandMatcher.pattern, matchResult)),
                            MessageFilter(formatRegex(it.suggestionMatcher.pattern, matchResult))
                        )
                    }.toMutableList()
                    settings.notificationSettings = it.notificationSettings.clone().also { notificationSettings ->
                        notificationSettings.notificationMatch.pattern = formatRegex(it.notificationSettings.notificationMatch.pattern, matchResult)
                    }
                }
            ).also {
                it.isAutoTab = true
                it.temporary = true
            }
            chatTab.updateServerSettings()
            tabSettings.tabs.add(chatTab)
            tabSettings.resetSortedChatTabs()
            return AutoTabData(it, chatTab)
        }
        return null
    }

    data class AutoTabData(
        val autoTabOptions: AutoTabOptions,
        val chatTab: ChatTab,
    )

    // replace all in autoPrefix %GROUP_1% %GROUP_2% etc with the corresponding group in the matched regex, start at 1 because 0 is the whole match
    private fun formatRegex(input: String, matchResult: MatchResult): String {
        val name = Minecraft.getInstance().player?.gameProfile?.name ?: ""
        var output = input.replace("%PLAYER%", name, ignoreCase = true)
        for (i in matchResult.groups.indices) {
            val groupValue = matchResult.groups[i]?.value ?: continue
            output = output.replace("%GROUP_$i%", groupValue)
        }
        output = output.replace(Regex("%GROUP_*?\\d+%"), "")
        return output
    }


    @Serializable
    class AutoTabOptions : MessageFilterFormatted {

        var skipOthersOnCreation: Boolean = true
        var tabNameFormatter: String = ""
        var regexFormatter: String = ""
        var autoSendFormatter: String = ""
        var autoPrefixFormatter: String = ""
        var priority: Int = 0
        var alwaysAdd: Boolean = false
        var skipOthers: Boolean = false
        var commandsOverrideAutoPrefix: Boolean = true
        var suggestionsPatterns: MutableList<ServerChatTabCommandSuggestion> = mutableListOf()
        var notificationSettings: ServerChatTabNotificationSettings = ServerChatTabNotificationSettings()
        var temporary: Boolean = true

        constructor(pattern: String) : super(pattern)

    }

}