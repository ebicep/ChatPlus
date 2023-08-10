package com.ebicep.chatplus.util

import net.minecraft.ChatFormatting
import java.awt.Color

fun Color.convertToArgb(): Int {
    return (this.alpha shl 24) or
            (this.red shl 16) or
            (this.green shl 8) or
            (this.blue)
}

enum class Colors(val red: Int, val green: Int, val blue: Int, val enumColor: ChatFormatting) {
    DEF(34, 34, 39, ChatFormatting.OBFUSCATED),
    BRONZE(144, 89, 35, ChatFormatting.OBFUSCATED),
    SILVER(148, 152, 161, ChatFormatting.OBFUSCATED),
    PLATINUM(229, 228, 226, ChatFormatting.OBFUSCATED),
    DIAMOND(185, 242, 255, ChatFormatting.OBFUSCATED),
    MASTER(255, 92, 51, ChatFormatting.OBFUSCATED),
    GRANDMASTER(143, 0, 0, ChatFormatting.OBFUSCATED),

    BLACK(0, 0, 0, ChatFormatting.BLACK),
    DARK_BLUE(0, 0, 170, ChatFormatting.DARK_BLUE),
    DARK_GREEN(0, 170, 0, ChatFormatting.DARK_GREEN),
    DARK_AQUA(0, 170, 170, ChatFormatting.DARK_AQUA),
    DARK_RED(170, 0, 0, ChatFormatting.DARK_RED),
    DARK_PURPLE(170, 0, 170, ChatFormatting.DARK_PURPLE),
    DARK_GRAY(85, 85, 85, ChatFormatting.DARK_GRAY),
    GOLD(255, 170, 0, ChatFormatting.GOLD),
    GRAY(170, 170, 170, ChatFormatting.GRAY),
    BLUE(85, 85, 255, ChatFormatting.BLUE),
    GREEN(85, 255, 85, ChatFormatting.GREEN),
    AQUA(85, 255, 255, ChatFormatting.AQUA),
    RED(255, 85, 85, ChatFormatting.RED),
    LIGHT_PURPLE(255, 85, 255, ChatFormatting.LIGHT_PURPLE),
    YELLOW(255, 255, 85, ChatFormatting.YELLOW),
    WHITE(255, 255, 255, ChatFormatting.WHITE),
    ORANGE(255, 140, 0, ChatFormatting.YELLOW)

    ;

    val FULL: Int = Color(red, green, blue, 255).convertToArgb()
    val ALPHA_100: Int = Color(red, green, blue, 100).convertToArgb()
    val ALPHA_200: Int = Color(red, green, blue, 200).convertToArgb()

    fun convertToArgb(alpha: Int = 255): Int {
        return (alpha shl 24) or
                (this.red shl 16) or
                (this.green shl 8) or
                (this.blue)
    }
}

