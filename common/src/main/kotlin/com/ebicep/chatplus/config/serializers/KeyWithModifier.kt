package com.ebicep.chatplus.config.serializers

import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.Serializable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

@Serializable
data class KeyWithModifier(
    @Serializable(with = KeySerializer::class)
    var key: InputConstants.Key,
    var modifier: Short
) {

    fun isDown(): Boolean {
        val keyDown = InputConstants.isKeyDown(Minecraft.getInstance().window.window, key.value)
        val modifierDown = modifier == 0.toShort() ||
                modifier == 1.toShort() && Screen.hasAltDown() ||
                modifier == 2.toShort() && Screen.hasControlDown() ||
                modifier == 4.toShort() && Screen.hasShiftDown()
        return keyDown && modifierDown
    }

}
