@file:UseSerializers(
    KeySerializer::class
)

package com.ebicep.chatplus.config

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.MOD_ID
import com.ebicep.chatplus.config.serializers.KeySerializer
import com.ebicep.chatplus.config.serializers.KeyWithModifier
import com.ebicep.chatplus.hud.BASE_Y_OFFSET
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderer
import com.ebicep.chatplus.hud.ChatTab
import com.ebicep.chatplus.translator.LanguageManager
import com.ebicep.chatplus.translator.RegexMatch
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    prettyPrint = true
}

var queueUpdateConfig = false

object Config {
    var values = ConfigVariables()

    fun save() {
        val configDirectory = File(ConfigDirectory.getConfigDirectory().toString())
        if (!configDirectory.exists()) {
            configDirectory.mkdir()
        }
        val configFile = File(configDirectory, "$MOD_ID.json")
        configFile.writeText(json.encodeToString(ConfigVariables.serializer(), values))
    }

    fun load() {
        ChatPlus.LOGGER.info("Config Directory: ${ConfigDirectory.getConfigDirectory().toAbsolutePath().normalize()}")
        val configDirectory = File(ConfigDirectory.getConfigDirectory().toString())
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
            values.chatTabs.add(ChatTab("All", "(?s).*"))
        }
        if (values.selectedTab >= values.chatTabs.size) {
            values.selectedTab = 0
        }
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
    // general
    var enabled: Boolean = true,
    var maxMessages: Int = 1000,
    var maxCommandSuggestions: Int = 15,
    var chatTimestampMode: TimestampMode = TimestampMode.HR_12_SECOND,
    var hoverHighlightEnabled: Boolean = true,
    var hoverHighlightColor: Int = 0,
    // tabs
    var chatTabs: MutableList<ChatTab> = mutableListOf(ChatTab("All", "(?s).*")),
    var selectedTab: Int = 0,
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
) {
    // variables here for custom setters

    // internal
    var x: Int = 0
        set(newX) {
            field = newX
            ChatRenderer.updateCachedDimension()
        }
    var y: Int = -BASE_Y_OFFSET
        set(newY) {
            field = newY
            ChatRenderer.updateCachedDimension()
        }
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

    // general
    var scale: Float = 1f
        set(newY) {
            field = newY
            ChatRenderer.updateCachedDimension()
        }
    var textOpacity: Float = 1f
        set(newY) {
            field = newY
            ChatRenderer.updateCachedDimension()
        }
    var backgroundOpacity: Float = .5f
        set(newY) {
            field = newY
            ChatRenderer.updateCachedDimension()
        }
    var lineSpacing: Float = 0f
        set(newY) {
            field = newY
            ChatRenderer.updateCachedDimension()
        }

}
