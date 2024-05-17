@file:UseSerializers(
    KeySerializer::class
)

package com.ebicep.chatplus.config

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.MOD_ID
import com.ebicep.chatplus.config.serializers.KeySerializer
import com.ebicep.chatplus.config.serializers.KeyWithModifier
import com.ebicep.chatplus.features.AlignMessage
import com.ebicep.chatplus.features.FilterHighlight
import com.ebicep.chatplus.features.HoverHighlight
import com.ebicep.chatplus.features.PlayerHeadChatDisplay
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_HEIGHT
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.ChatTabs.defaultTab
import com.ebicep.chatplus.features.internal.MessageFilter
import com.ebicep.chatplus.features.speechtotext.SpeechToText
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderer
import com.ebicep.chatplus.hud.EDIT_BOX_HEIGHT
import com.ebicep.chatplus.translator.LanguageManager
import com.ebicep.chatplus.translator.RegexMatch
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import net.minecraft.util.Mth
import java.awt.Color
import java.io.File

private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    prettyPrint = true
}
val configDirectoryPath: String
    get() = ConfigDirectory.getConfigDirectory().toString() + "\\chatplus"
var queueUpdateConfig = false


object Config {
    var values = ConfigVariables()

    fun resetSortedChatTabs() {
        values.sortedChatTabs = values.chatTabs.sortedBy { -it.priority }
    }

    fun save() {
        val configDirectory = File(configDirectoryPath)
        if (!configDirectory.exists()) {
            configDirectory.mkdir()
        }
        val configFile = File(configDirectory, "$MOD_ID.json")
        configFile.writeText(json.encodeToString(ConfigVariables.serializer(), values))
    }

    fun load() {
        ChatPlus.LOGGER.info("Config Directory: ${ConfigDirectory.getConfigDirectory().toAbsolutePath().normalize()}\\chatplus")
        val configDirectory = File(configDirectoryPath)
        if (!configDirectory.exists()) {
            return
        }
        val configFile = File(configDirectory, "$MOD_ID.json")
        if (configFile.exists()) {
            val json = Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
            values = json.decodeFromString(ConfigVariables.serializer(), configFile.readText())
            correctValues()
            loadValues()
        }
    }

    private fun loadValues() {
        values.chatTabs.forEach {
            it.regex = Regex(it.pattern)
        }
        resetSortedChatTabs()
        values.filterHighlights.forEach {
            it.regex = Regex(it.pattern)
        }
        values.autoBookMarkPatterns.forEach {
            it.regex = Regex(it.pattern)
        }
        LanguageManager.updateTranslateLanguages()
        SpeechToText.updateTranslateLanguage()
    }

    private fun correctValues() {
        if (values.maxMessages < MIN_MAX_MESSAGES) {
            values.maxMessages = MIN_MAX_MESSAGES
        } else if (values.maxMessages > MAX_MAX_MESSAGES) {
            values.maxMessages = MAX_MAX_MESSAGES
        }
        if (values.maxCommandSuggestions < MIN_MAX_COMMAND_SUGGESTIONS) {
            values.maxCommandSuggestions = MIN_MAX_COMMAND_SUGGESTIONS
        } else if (values.maxCommandSuggestions > MAX_MAX_COMMAND_SUGGESTIONS) {
            values.maxCommandSuggestions = MAX_MAX_COMMAND_SUGGESTIONS
        }
        if (values.chatTabs.isEmpty()) {
            values.chatTabs.add(defaultTab)
        }
        values.selectedTab = Mth.clamp(values.selectedTab, 0, values.chatTabs.size - 1)
        LanguageManager.findLanguageFromName(values.translateTo).let { if (it == null) values.translateTo = "Auto Detect" }
        LanguageManager.findLanguageFromName(values.translateSelf).let { if (it == null) values.translateSelf = "Auto Detect" }
        LanguageManager.findLanguageFromName(values.translateSpeak).let { if (it == null) values.translateSpeak = "English" }
        LanguageManager.findLanguageFromName(values.speechToTextTranslateLang)
            .let { if (it == null) values.speechToTextTranslateLang = "English" }
        save()
    }

}

const val MIN_MAX_MESSAGES = 1000
const val MAX_MAX_MESSAGES = 10_000_000
const val MIN_MAX_COMMAND_SUGGESTIONS = 10
const val MAX_MAX_COMMAND_SUGGESTIONS = 30

@Serializable
data class ConfigVariables(
    var x: Int = 0,
    var y: Int = -CHAT_TAB_HEIGHT - EDIT_BOX_HEIGHT,
    // general
    var enabled: Boolean = true,
    var scale: Float = 1f,
    var textOpacity: Float = 1f,
    var backgroundOpacity: Float = .5f,
    var unfocusedHeight: Float = .5f,
    var lineSpacing: Float = 0f,
    var maxMessages: Int = 1000,
    var maxCommandSuggestions: Int = 15,
    var chatTimestampMode: TimestampMode = TimestampMode.HR_12_SECOND,
    // tabs
    var chatTabs: MutableList<ChatTab> = mutableListOf(defaultTab),
    var selectedTab: Int = 0,
    var scrollCycleTabEnabled: Boolean = true,
    var arrowCycleTabEnabled: Boolean = true,
    // filter highlight
    var filterHighlightEnabled: Boolean = true,
    var filterHighlights: MutableList<FilterHighlight.Filter> = mutableListOf(),
    // hover highlight
    var hoverHighlightEnabled: Boolean = true,
    var hoverHighlightMode: HoverHighlight.HighlightMode = HoverHighlight.HighlightMode.BRIGHTER,
    var hoverHighlightColor: Int = 419430400,
    // bookmark
    var bookmarkEnabled: Boolean = true,
    var bookmarkColor: Int = Color(217, 163, 67, 200).rgb,
    var bookmarkKey: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.b"), 2),
    var bookmarkTextBarElementEnabled: Boolean = true,
    var bookmarkTextBarElementKey: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.b"), 2),
    var autoBookMarkPatterns: MutableList<MessageFilter> = mutableListOf(),
    // find message
    var findMessageEnabled: Boolean = true,
    var findMessageHighlightInputBox: Boolean = false,
    var findMessageTextBarElementEnabled: Boolean = true,
    var findMessageKey: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.f"), 2),
    // screen shot chat
    var screenshotChatEnabled: Boolean = true,
    var screenshotChatLine: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.s"), 2),
    var screenshotChatAutoUpload: Boolean = true,
    // player head chat display
    var playerHeadChatDisplayEnabled: Boolean = true,
    var playerHeadChatDisplayShowOnWrapped: Boolean = false,
    var playerHeadChatDisplayOffsetNonHeadMessages: Boolean = false,
    var playerHeadChatDisplayOffsetNonHeadMessagesShowOnWrapped: Boolean = true,
    // keys binds
    var keyNoScroll: InputConstants.Key = InputConstants.getKey("key.keyboard.left.control"),
    var keyFineScroll: InputConstants.Key = InputConstants.getKey("key.keyboard.left.shift"),
    var keyLargeScroll: InputConstants.Key = InputConstants.getKey("key.keyboard.left.alt"),
    var keyMoveChat: InputConstants.Key = InputConstants.getKey("key.keyboard.right.control"),
    var keyCopyMessageWithModifier: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.c"), 2),
    var copyNoFormatting: Boolean = true,
    var keyPeekChat: InputConstants.Key = InputConstants.getKey("key.keyboard.p"),
    // translator
    var translatorEnabled: Boolean = true,
    var translatorRegexes: MutableList<RegexMatch> = mutableListOf(),
    var translateTo: String = "Auto Detect",
    var translateSelf: String = "Auto Detect",
    var translateSpeak: String = "English",
    var translateKeepOnAfterChatClose: Boolean = true,
    var translateKey: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.t"), 2),
    var translateToggleKey: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.t"), 2),
    var translateClickEnabled: Boolean = true,
    // speech to text
    var speechToTextEnabled: Boolean = true,
    var speechToTextToInputBox: Boolean = true,
    var speechToTextMicrophoneKey: InputConstants.Key = InputConstants.getKey("key.keyboard.b"),
    var speechToTextQuickSendKey: InputConstants.Key = InputConstants.getKey("key.keyboard.enter"),
    var speechToTextTranslateEnabled: Boolean = false,
    var speechToTextTranslateToInputBox: Boolean = true,
    var speechToTextTranslateLang: String = "English",
) {
    // internal
    @Transient
    var sortedChatTabs: List<ChatTab> = listOf()
    var width: Int = 180
        set(newWidth) {
            if (field == newWidth) {
                return
            }
            field = newWidth
            queueUpdateConfig = true
            ChatManager.selectedTab.rescaleChat()
        }

    // variables here for custom setters

    var height: Int = 320
        set(newHeight) {
            if (field == newHeight) {
                return
            }
            field = newHeight
            queueUpdateConfig = true
            ChatManager.selectedTab.rescaleChat()
        }

    // general
    var messageAlignment: AlignMessage.Alignment = AlignMessage.Alignment.LEFT
        set(newAlignment) {
            if (field == newAlignment) {
                return
            }
            field = newAlignment
            queueUpdateConfig = true
            PlayerHeadChatDisplay.updateMessageOffset()
        }

    // tabs
    var chatTabsEnabled: Boolean = true
        set(newY) {
            if (field == newY) {
                return
            }
            field = newY
            ChatRenderer.updateCachedDimension()
            queueUpdateConfig = true
        }

    // speech to text
    var speechToTextSampleRate: Int = 48000
        set(value) {
            if (field == value) {
                return
            }
            SpeechToText.microphoneThread.resetMicrophone()
            SpeechToText.microphoneThread.resetRecognizer()
            field = value
            queueUpdateConfig = true
        }
    var speechToTextMicrophone: String = "Default"
        set(value) {
            if (field == value) {
                return
            }
            SpeechToText.microphoneThread.resetMicrophone()
            field = value
            queueUpdateConfig = true
        }
    var speechToTextSelectedAudioModel: String = ""
        set(value) {
            if (field == value) {
                return
            }
            SpeechToText.microphoneThread.resetRecognizer()
            field = value
            queueUpdateConfig = true
        }
}
