@file:UseSerializers(
    KeySerializer::class
)

package com.ebicep.chatplus.config

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.MOD_ID
import com.ebicep.chatplus.config.serializers.KeySerializer
import com.ebicep.chatplus.config.serializers.KeyWithModifier
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatTab
import com.ebicep.chatplus.hud.baseYOffset
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import java.io.File

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
        if (values.maxMessages < minMaxMessages) {
            values.maxMessages = minMaxMessages
        } else if (values.maxMessages > maxMaxMessages) {
            values.maxMessages = maxMaxMessages
        }
        if (values.chatTabs.isEmpty()) {
            values.chatTabs.add(ChatTab("All", "(?s).*"))
        }
        if (values.selectedTab >= values.chatTabs.size) {
            values.selectedTab = 0
        }
        save()
    }

}

val minMaxMessages = 1000
val maxMaxMessages = 10_000_000

@Serializable
data class ConfigVariables(
    var enabled: Boolean = true,
    var x: Int = 0,
    var y: Int = -baseYOffset,
    var height: Int = 180,
    var width: Int = 320,
    var scale: Float = 1f,
    var maxMessages: Int = 1000,
    var textOpacity: Float = 1f,
    var backgroundOpacity: Float = .5f,
    var lineSpacing: Float = 0f,
    var chatTimestampMode: TimestampMode = TimestampMode.HR_12_SECOND,
    var chatTabs: MutableList<ChatTab> = mutableListOf(ChatTab("All", "(?s).*")),
    var selectedTab: Int = 0,

    //keys
    var keyNoScroll: InputConstants.Key = InputConstants.getKey("key.keyboard.left.control"),
    var keyFineScroll: InputConstants.Key = InputConstants.getKey("key.keyboard.left.shift"),
    var keyLargeScroll: InputConstants.Key = InputConstants.getKey("key.keyboard.left.alt"),
    var keyMoveChat: InputConstants.Key = InputConstants.getKey("key.keyboard.right.control"),
    var keyCopyMessageWithModifier: KeyWithModifier = KeyWithModifier(InputConstants.getKey("key.keyboard.c"), 2),

    ) {

    @Transient
    var chatWidth: Int = width
        set(newWidth) {
            field = newWidth
            width = newWidth
            queueUpdateConfig = true
            ChatManager.selectedTab.rescaleChat()
        }

    @Transient
    var chatHeight: Int = height
        set(newHeight) {
            field = newHeight
            height = newHeight
            queueUpdateConfig = true
            ChatManager.selectedTab.rescaleChat()
        }

//    fun getKeyCopyMessageModifier(): InputConstants.Key? {
//        when(keyCopyMessageModifier) {
//            1 -> return InputConstants.getKey("key.keyboard.left.shift")
//            1 -> return InputConstants.getKey("key.keyboard.left.shift")
//            1 -> return InputConstants.getKey("key.keyboard.left.shift")
//            else -> return null
//        }
//    }
}
