@file:UseSerializers(
    KeySerializer::class
)

package com.ebicep.chatplus.config

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.MOD_ID
import com.ebicep.chatplus.config.serializers.KeySerializer
import com.ebicep.chatplus.config.serializers.KeyWithModifier
import com.ebicep.chatplus.features.AlignText
import com.ebicep.chatplus.features.FilterHighlight
import com.ebicep.chatplus.features.chattabs.CHAT_TAB_HEIGHT
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chattabs.ChatTabs.defaultTab
import com.ebicep.chatplus.features.speechtotext.SpeechToText
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderer
import com.ebicep.chatplus.hud.EDIT_BOX_HEIGHT
import com.ebicep.chatplus.translator.LanguageManager
import com.ebicep.chatplus.translator.RegexMatch
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import net.minecraft.util.Mth
import java.io.File
import java.util.*

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
        values.filterHighlights.forEach {
            it.regex = Regex(it.pattern)
        }
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
    var lineSpacing: Float = 0f,
    var maxMessages: Int = 1000,
    var maxCommandSuggestions: Int = 15,
    var chatTimestampMode: TimestampMode = TimestampMode.HR_12_SECOND,
    var hoverHighlightEnabled: Boolean = true,
    var hoverHighlightColor: Int = 0,
    var textAlignment: AlignText.Alignment = AlignText.Alignment.LEFT,
    // tabs
    var chatTabs: MutableList<ChatTab> = mutableListOf(defaultTab),
    var selectedTab: Int = 0,
    var scrollCycleTabEnabled: Boolean = true,
    var arrowCycleTabEnabled: Boolean = true,
    // filter highlight
    var filterHighlightEnabled: Boolean = true,
    var filterHighlights: MutableList<FilterHighlight.Filter> = mutableListOf(),
    // screen shot chat
    var screenshotChatEnabled: Boolean = true,
    var screenshotChatLine: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.s"), 2),
    var screenshotChatAutoUpload: Boolean = true,
    // keys binds
    var keyNoScroll: InputConstants.Key = InputConstants.getKey("key.keyboard.left.control"),
    var keyFineScroll: InputConstants.Key = InputConstants.getKey("key.keyboard.left.shift"),
    var keyLargeScroll: InputConstants.Key = InputConstants.getKey("key.keyboard.left.alt"),
    var keyMoveChat: InputConstants.Key = InputConstants.getKey("key.keyboard.right.control"),
    var keyCopyMessageWithModifier: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.c"), 2),
    var copyNoFormatting: Boolean = true,
    // translator
    var translatorEnabled: Boolean = true,
    var translatorRegexes: MutableList<RegexMatch> = mutableListOf(),
    var translateTo: String = "Auto Detect",
    var translateSelf: String = "Auto Detect",
    var translateSpeak: String = "English",
    // speech to text
    var speechToTextEnabled: Boolean = true,
    var speechToTextMicrophoneKey: InputConstants.Key = InputConstants.getKey("key.keyboard.b"),
    var speechToTextQuickSendKey: InputConstants.Key = InputConstants.getKey("key.keyboard.enter"),
) {
    // variables here for custom setters

    // internal
    var width: Int = 180
        set(newWidth) {
            field = newWidth
            queueUpdateConfig = true
            ChatManager.selectedTab.rescaleChat()
        }
    var height: Int = 320
        set(newHeight) {
            field = newHeight
            queueUpdateConfig = true
            ChatManager.selectedTab.rescaleChat()
        }

    // tabs
    var chatTabsEnabled: Boolean = true
        set(newY) {
            field = newY
            ChatRenderer.updateCachedDimension()
            queueUpdateConfig = true
        }

    // speech to text
    var speechToTextSampleRate: Int = 48000
        set(value) {
            if (field != value) {
                SpeechToText.microphoneThread.resetMicrophone()
                SpeechToText.microphoneThread.resetRecognizer()
            }
            field = value
            queueUpdateConfig = true
        }
    var speechToTextMicrophone: String = "Default"
        set(value) {
            if (field != value) {
                SpeechToText.microphoneThread.resetMicrophone()
            }
            field = value
            queueUpdateConfig = true
        }
    var speechToTextSelectedAudioModel: String = ""
        set(value) {
            if (field != value) {
                SpeechToText.microphoneThread.resetRecognizer()
            }
            field = value
            queueUpdateConfig = true
        }
}
