package com.ebicep.chatplus.config.migration

import com.ebicep.chatplus.MOD_ID
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.MessageDirection
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.features.internal.MessageFilter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable


object V1 : Migrator<SchemaV1> {

    override fun getFileNameVersion(): String {
        return MOD_ID
    }

    override fun getSerializer(): KSerializer<SchemaV1> {
        return SchemaV1.serializer()
    }

    override fun migrate(old: SchemaV1) {
        val values = Config.values
        values.chatWindows.add(ChatWindow().also {
            it.renderer.x = old.x
            it.renderer.y = old.y
            it.renderer.width = old.width
            it.renderer.height = old.height

            it.generalSettings.scale = old.scale
            it.generalSettings.textOpacity = old.textOpacity
            it.generalSettings.unfocusedHeight = old.unfocusedHeight
            it.generalSettings.lineSpacing = old.lineSpacing
            it.generalSettings.messageDirection = old.messageDirection

            it.tabSettings.tabs = old.chatTabs
        })
        values.translatorRegexes = old.translatorRegexes.map { MessageFilter(it.match) }.toMutableList()
    }

}

@Serializable
data class SchemaV1(

    var x: Int = 0,
    var y: Int = 0,
    var width: Int = 0,
    var height: Int = 0,

    var scale: Float = 1f,
    var textOpacity: Float = 1f,
    var backgroundOpacity: Float = .5f,
    var unfocusedHeight: Float = .5f,
    var lineSpacing: Float = 0f,
    var messageDirection: MessageDirection = MessageDirection.BOTTOM_UP,

    var chatTabs: MutableList<ChatTab> = mutableListOf(),

    var translatorRegexes: MutableList<SchemaV0TranslatorRegex> = mutableListOf()

)

@Serializable
data class SchemaV0TranslatorRegex(
    val match: String = "",
    val senderNameGroupIndex: Int = 0
)