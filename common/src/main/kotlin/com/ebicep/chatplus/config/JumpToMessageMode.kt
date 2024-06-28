package com.ebicep.chatplus.config

import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component

@Serializable
enum class JumpToMessageMode(key: String) {
    TOP("chatPlus.chatSettings.jumpToMessageMode.top"),
    MIDDLE("chatPlus.chatSettings.jumpToMessageMode.middle"),
    BOTTOM("chatPlus.chatSettings.jumpToMessageMode.bottom"),
    CURSOR("chatPlus.chatSettings.jumpToMessageMode.cursor"),

    ;

    val translatable: Component = Component.translatable(key)

}