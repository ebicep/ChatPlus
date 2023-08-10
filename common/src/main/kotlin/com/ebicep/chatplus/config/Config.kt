package com.ebicep.chatplus.config


import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.MOD_ID
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatTab
import com.ebicep.warlordsplusplus.config.ConfigDirectory
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import java.io.File

private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    prettyPrint = true
}

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
        }
    }

}

val minMaxMessages = 1000
val maxMaxMessages = 10_000_000

@Serializable
data class ConfigVariables(
    var enabled: Boolean = true,
    var x: Int = 0,
    var y: Int = -ChatManager.baseYOffset,
    var height: Int = 180,
    var width: Int = 320,
    var scale: Float = 1f,
    var maxMessages: Int = 1000,
    var textOpacity: Float = 1f,
    var backgroundOpacity: Float = .5f,
    var lineSpacing: Float = 0f,
    var chatTimestampMode: TimestampMode = TimestampMode.HR_12_SECOND,
    var chatTabs: List<ChatTab> = listOf(ChatTab("All", "(?s).*"))
) {
    @Transient
    var chatWidth: Int = width
        set(width) {
            field = width
            ChatManager.selectedTab.rescaleChat()
        }

    @Transient
    var chatHeight: Int = height
        set(height) {
            field = height
            ChatManager.selectedTab.rescaleChat()
        }
}
