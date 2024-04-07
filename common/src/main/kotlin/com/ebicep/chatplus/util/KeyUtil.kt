package com.ebicep.chatplus.util

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft

object KeyUtil {

    fun InputConstants.Key.isDown(): Boolean {
        val window = Minecraft.getInstance().window ?: return false
        return InputConstants.isKeyDown(window.window, value)
    }

}