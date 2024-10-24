package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.features.internal.MessageFilterFormatted
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.LiteralContents

@Serializable
class AutoTabCreator {

    var autoTabOptions: MutableList<AutoTabOptions> = mutableListOf()

    @Transient
    lateinit var chatWindow: ChatWindow

    init {
        autoTabOptions.forEach {
            it.updateRegex()
        }
        EventBus.register<AddNewMessageEvent> {
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
        if (component.contents !is LiteralContents) {
            return null
        }
        val text = component.string
        val tabSettings = chatWindow.tabSettings
        // check if already in other tab
        if (tabSettings.tabs.any { it.isAutoTab && it.matches(text) }) {
            return null
        }
        autoTabOptions.forEach {
            // check matches and get group index
            val matchResult: MatchResult = it.find(text) ?: return@forEach
            val tabName = formatRegex(it.tabNameFormatter, matchResult)
            val pattern = formatRegex(it.regexFormatter, matchResult)
            val autoPrefix = formatRegex(it.autoPrefixFormatter, matchResult)
            val chatTab = ChatTab(
                chatWindow,
                tabName,
                pattern,
                autoPrefix,
                it.priority,
                it.alwaysAdd,
                it.skipOthers,
                it.commandsOverrideAutoPrefix,
                it.temporary,
                true
            )
            tabSettings.tabs.add(chatTab)
            tabSettings.resetSortedChatTabs()
            return AutoTabData(it, chatTab)
        }
        return null
    }

    data class AutoTabData(
        val autoTabOptions: AutoTabOptions,
        val chatTab: ChatTab
    )

    // replace all in autoPrefix %GROUP_1% %GROUP_2% etc with the corresponding group in the matched regex, start at 1 because 0 is the whole match
    private fun formatRegex(input: String, matchResult: MatchResult): String {
        var output = input
        for (i in matchResult.groups.indices) {
            val groupValue = matchResult.groups[i]?.value ?: continue
            output = output.replace("%GROUP_$i%", groupValue)
        }
        return output
    }


    @Serializable
    class AutoTabOptions : MessageFilterFormatted {

        var skipOthersOnCreation: Boolean = true
        var tabNameFormatter: String = ""
        var regexFormatter: String = ""
        var autoPrefixFormatter: String = ""
        var priority: Int = 0
        var alwaysAdd: Boolean = false
        var skipOthers: Boolean = false
        var commandsOverrideAutoPrefix: Boolean = true
        var temporary: Boolean = true

        constructor(pattern: String) : super(pattern) {
        }

    }

}