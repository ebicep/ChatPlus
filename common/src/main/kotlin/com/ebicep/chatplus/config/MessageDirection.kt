package com.ebicep.chatplus.config

import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component

@Serializable
enum class MessageDirection(key: String) : EnumTranslatableName {
    TOP_DOWN("chatPlus.chatWindow.messageDirection.topDown"),
    BOTTOM_UP("chatPlus.chatWindow.messageDirection.bottomUp"),

    ;

    val translatable: Component = Component.translatable(key)

    override fun getTranslatableName(): Component {
        return translatable
    }

}