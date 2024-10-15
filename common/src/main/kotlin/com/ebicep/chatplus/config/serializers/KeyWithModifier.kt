package com.ebicep.chatplus.config.serializers

import com.ebicep.chatplus.util.KeyUtil.isDown
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.Serializable
import net.minecraft.client.gui.screens.Screen

@Serializable
data class KeyWithModifier(
    @Serializable(with = KeySerializer::class)
    var key: InputConstants.Key,
    var modifier: Short
) {

    fun isDown(): Boolean {
        val keyDown = key.isDown()
        val modifierDown = modifier == 0.toShort() ||
                modifier == 1.toShort() && Screen.hasAltDown() ||
                modifier == 2.toShort() && Screen.hasControlDown() ||
                modifier == 4.toShort() && Screen.hasShiftDown()
        return keyDown && modifierDown
    }

    fun isDown(keyCode: Int, modifier: Int): Boolean {
        return key.value != -1 && key.value == keyCode && (this.modifier == 0.toShort() || this.modifier == modifier.toShort())
    }

}
