package com.ebicep.chatplus.config

import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component

@Serializable
enum class MessageDirection(key: String) {
    TOP_DOWN("chatPlus.chatSettings.messageDirection.topDown"),
    BOTTOM_UP("chatPlus.chatSettings.messageDirection.bottomUp"),

    ;

    val translatable: Component = Component.translatable(key)

}