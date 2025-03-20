@file:UseSerializers(
    KeySerializer::class
)

package com.ebicep.chatplus.config

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.ChatPlusPlatformInit
import com.ebicep.chatplus.MOD_ID
import com.ebicep.chatplus.config.migration.MigrationManager
import com.ebicep.chatplus.config.serializers.KeySerializer
import com.ebicep.chatplus.config.serializers.KeyWithModifier
import com.ebicep.chatplus.features.*
import com.ebicep.chatplus.features.MovableChat.InputBoxSettings
import com.ebicep.chatplus.features.chattabs.TabNotificationSettings
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.features.chatwindows.ChatWindowsManager.createDefaultWindow
import com.ebicep.chatplus.features.internal.MessageFilter
import com.ebicep.chatplus.features.internal.MessageFilterFormatted
import com.ebicep.chatplus.features.speechtotext.MicrophoneThread.SpeechToTextReplace
import com.ebicep.chatplus.features.speechtotext.SpeechToText
import com.ebicep.chatplus.translator.LanguageManager
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import net.minecraft.util.Mth
import java.awt.Color
import java.io.File
import kotlin.math.max

const val CONFIG_NAME = "${MOD_ID}-v2.5.0.json"
val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    prettyPrint = true
}
val configDirectoryPath: String
    get() = ConfigDirectory.getConfigDirectory().toString() + "/chatplus"
var queueUpdateConfig = false


object Config {
    var values = ConfigVariables()
        get() {
            if (!ChatPlus.initialized) {
                ChatPlusPlatformInit.platformInit()
            }
            return field
        }
    var loaded = false

    fun save() {
        val configDirectory = File(configDirectoryPath)
        if (!configDirectory.exists()) {
            configDirectory.mkdir()
        }
        val configFile = File(configDirectory, CONFIG_NAME)
        configFile.writeText(json.encodeToString(ConfigVariables.serializer(), values))
    }

    fun load() {
        ChatPlus.LOGGER.info("Config Directory: ${ConfigDirectory.getConfigDirectory().toAbsolutePath().normalize()}/chatplus")
        val configDirectory: File = File(configDirectoryPath)
        if (!configDirectory.exists()) {
            configDirectory.mkdir()
        }
        val configFile: File = File(configDirectory, CONFIG_NAME)
        if (!configFile.exists()) {
            ChatPlus.LOGGER.info("No config file found, checking migration")
            if (!MigrationManager.tryMigration(configDirectory, configFile)) {
                ChatPlus.LOGGER.info("No migration found, creating new config")
                configFile.createNewFile()
                configFile.writeText(json.encodeToString(ConfigVariables.serializer(), values))
            }
        } else {
            values = json.decodeFromString(ConfigVariables.serializer(), configFile.readText())
        }
        correctValues()
        loadValues()
        loaded = true
        ChatPlus.LOGGER.info("Config Loaded")
    }

    private fun loadValues() {
        values.filterMessagesPatterns.forEach {
            it.updateRegex()
        }
        values.autoBookMarkPatterns.forEach {
            it.updateRegex()
        }
        LanguageManager.updateTranslateLanguages()
        SpeechToText.updateTranslateLanguage()
        values.speechToTextReplace.forEach {
            it.updateRegex()
        }
        try {
            charset(values.speechToTextCharset)
        } catch (_: Exception) {
            values.speechToTextCharset = "UTF-8"
        }
    }

    private fun correctValues() {
        values.maxCommandSuggestions = Mth.clamp(values.maxCommandSuggestions, 10, 30)
        values.inputBoxSettings.maxInputBoxInputLength = max(1, values.inputBoxSettings.maxInputBoxInputLength)
        if (values.chatWindows.isEmpty()) {
            values.chatWindows.add(createDefaultWindow())
        }
        LanguageManager.findLanguageFromName(values.translateTo).let { if (it == null) values.translateTo = "Auto Detect" }
        LanguageManager.findLanguageFromName(values.translateSelf).let { if (it == null) values.translateSelf = "Auto Detect" }
        LanguageManager.findLanguageFromName(values.translateSpeak).let { if (it == null) values.translateSpeak = "English" }
        LanguageManager.findLanguageFromName(values.speechToTextTranslateLang).let { if (it == null) values.speechToTextTranslateLang = "English" }
        save()
    }

}

@Serializable
data class ConfigVariables(
    // general
    var globalizedConfig: Boolean = true,
    var enabled: Boolean = true,
    var addMessagesIfDisabled: Boolean = false,
    var showVanillaWhenUnfocused: Boolean = false,
    var vanillaInputBox: Boolean = false,
    var saveInputBoxMessage: Boolean = false,
    var wrappedMessageLineIndent: Int = 0,
    var maxMessages: Int = -1,
    var maxCommandSuggestions: Int = 15,
    var jumpToMessageMode: JumpToMessageMode = JumpToMessageMode.CURSOR,
    var selectChatLinePriority: Int = 100,
    var timestampSettings: TimestampMessages.TimestampSettings = TimestampMessages.TimestampSettings(),
    var inputOverFlowAutoFillSettings: InputOverFlowAutoFill.InputOverFlowAutoFillSettings = InputOverFlowAutoFill.InputOverFlowAutoFillSettings(),
    var messageImagePreviewSettings: MessageImagePreview.MessageImagePreviewSettings = MessageImagePreview.MessageImagePreviewSettings(),
    // hide chat
    var hideChatEnabled: Boolean = false,
    var hideChatShowWhenFocused: Boolean = true,
    var hideChatShowHiddenOnScreen: Boolean = true,
    var hideChatToggleKey: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.unknown"), 0),
    var alwaysShowChat: Boolean = false,
    var alwaysShowChatToggleKey: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.unknown"), 0),
    // compact messages
    var compactMessagesEnabled: Boolean = true,
    var compactMessagesRefreshAddedTime: Boolean = false,
    var compactMessagesSearchAmount: Int = 1,
    var compactMessageComparatorMode: CompactMessages.CompactComparatorMode = CompactMessages.CompactComparatorMode.VANILLA,
    var compactMessageSettings: CompactMessages.CompactMessageCustomSettings = CompactMessages.CompactMessageCustomSettings(),
    // scrollbar
    var scrollbarEnabled: Boolean = true,
    var invertedScrolling: Boolean = false,
    var scrollbarColor: Int = Color(128, 134, 139, 255).rgb,
    var scrollbarWidth: Int = 6,
    // animation
    var animationEnabled: Boolean = true,
    var animationDisableOnFocus: Boolean = false,
    var animationNewMessageTransitionTime: Int = 200,
    // windows
    var tabEditorScreen: Boolean = true,
    var windowEditorScreen: Boolean = true,
    var scrollCycleTabEnabled: Boolean = true,
    var arrowCycleTabEnabled: Boolean = true,
    var moveToTabWhenCycling: Boolean = true,
    var inputBoxAutoAdjustChatWindowEnabled: Boolean = true,
    var tabNotificationSettings: TabNotificationSettings = TabNotificationSettings(),
    // moving chat
    var movableChatEnabled: Boolean = true,
    var movableChatShowEnabledOnScreen: Boolean = true,
    var movableChatKey: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.right.control"), 0),
    var movableChatColor: Int = Color(255, 255, 255, 200).rgb,
    var movableChatSelectedColor: Int = Color(0, 255, 0, 200).rgb,
    var movableChatToggleTextBarElement: Boolean = false, // TODO add enabled
    var inputBoxSettings: InputBoxSettings = InputBoxSettings(),
    // notes
    var sendNoteEnabled: Boolean = true,
    var sendNoteKey: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.enter"), 4),
    var sendNoteClickMode: SendNote.NoteClickMode = SendNote.NoteClickMode.APPEND_INPUT,
    var sendNoteSelectMode: SendNote.NoteSelectMode = SendNote.NoteSelectMode.LINE,
    var sendNoteSelectKey: InputConstants.Key = InputConstants.getKey("key.keyboard.left.shift"),
    var sendNoteSelectModeKey: SendNote.NoteSelectMode = SendNote.NoteSelectMode.WHOLE_MESSAGE,
    var sendNoteTextBarElementEnabled: Boolean = true,
    // filter highlight
    var filterMessagesEnabled: Boolean = true,
    var filterMessagesLinePriority: Int = 150,
    var filterMessagesPatterns: MutableList<FilterMessages.Filter> = mutableListOf(),
    // hover highlight
    var hoverHighlightEnabled: Boolean = true,
    var hoverHighlightLinePriority: Int = 0,
    var hoverHighlightMode: HoverHighlight.HighlightMode = HoverHighlight.HighlightMode.BRIGHTER,
    var hoverHighlightColor: Int = 419430400,
    // bookmark
    var bookmarkEnabled: Boolean = true,
    var bookmarkLinePriority: Int = 30,
    var bookmarkColor: Int = Color(217, 163, 67, 200).rgb,
    var bookmarkKey: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.b"), 2),
    var bookmarkTextBarElementEnabled: Boolean = true,
    var bookmarkTextBarElementKey: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.b"), 2),
    var autoBookMarkPatterns: MutableList<MessageFilterFormatted> = mutableListOf(),
    // find message
    var findMessageEnabled: Boolean = true,
    var findMessageHighlightInputBox: Boolean = false,
    var findMessageHighlightMatchedText: Boolean = true,
    var findMessageIgnoreCase: Boolean = true,
    var findMessageLinePriority: Int = 250,
    var findMessageKey: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.f"), 2),
    var findMessageDefaultMode: FindMessage.FindMode = FindMessage.FindMode.CONTAINS,
    var findMessageTextBarElementEnabled: Boolean = true,
    // copy message
    var copyMessageKey: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.c"), 2),
    var copyMessageLinePriority: Int = 50,
    var copyNoFormatting: Boolean = true,
    var copyMessageFormattingSymbolOverride: String = "",
    var copyMessageSeparator: String = "\n",
    // delete message
    var deleteMessageEnabled: Boolean = true,
    var deleteMessageKey: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.d"), 2),
    // screen shot chat
    var screenshotChatEnabled: Boolean = true,
    var screenshotChatCopyToClipboard: Boolean = true,
    var screenshotChatSaveToFile: Boolean = true, // TODO
    var screenshotChatAutoUpload: Boolean = true,
    var screenshotChatLinePriority: Int = 200,
    var screenshotChatKey: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.s"), 2),
    var screenshotDefaultScreenShotMode: ScreenshotChat.ScreenshotMode = ScreenshotChat.ScreenshotMode.CURRENT_WINDOW,
    var screenshotDefaultScreenBackgroundMode: ScreenshotChat.ScreenshotBackgroundMode = ScreenshotChat.ScreenshotBackgroundMode.TRANSPARENT,
    var screenshotDefaultScreenShotWindowsMode: ScreenshotChat.ScreenshotWindowsMode = ScreenshotChat.ScreenshotWindowsMode.SPLIT,
    var screenshotChatTextBarElementEnabled: Boolean = true,
    // player head chat display
    var playerHeadChatDisplayEnabled: Boolean = true,
    var playerHeadChatDisplayShowOnWrapped: Boolean = false,
    var playerHeadChatDisplayOffsetNonHeadMessages: Boolean = false,
    var playerHeadChatDisplayOffsetNonHeadMessagesShowOnWrapped: Boolean = true,
    // keys binds
    var keyNoScroll: InputConstants.Key = InputConstants.getKey("key.keyboard.left.control"),
    var keyFineScroll: InputConstants.Key = InputConstants.getKey("key.keyboard.left.shift"),
    var keyLargeScroll: InputConstants.Key = InputConstants.getKey("key.keyboard.left.alt"),
    var keyPeekChat: InputConstants.Key = InputConstants.getKey("key.keyboard.p"),
    // translator
    var translatorEnabled: Boolean = true,
    var translatorTextBarElementEnabled: Boolean = true,
    var translatorRegexes: MutableList<MessageFilter> = mutableListOf(),
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
    var speechToTextCharset: String = "UTF-8",
    var speechToTextMicrophoneKey: InputConstants.Key = InputConstants.getKey("key.keyboard.b"),
    var speechToTextQuickSendKey: InputConstants.Key = InputConstants.getKey("key.keyboard.enter"),
    var speechToTextTranslateEnabled: Boolean = false,
    var speechToTextTranslateToInputBox: Boolean = true,
    var speechToTextTranslateLang: String = "English",
    var speechToTextAutoReplacePlayers: Boolean = true,
    var speechToTextAutoReplacePlayersMaxSearchDepth: Int = 3,
    var speechToTextReplace: MutableList<SpeechToTextReplace> = mutableListOf(),
) {

    var chatWindows: MutableList<ChatWindow> = mutableListOf()
        set(value) {
            field = if (value.isEmpty()) {
                mutableListOf(createDefaultWindow())
            } else {
                value
            }
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
