package com.ebicep.chatplus.util

import net.minecraft.client.Minecraft

object ClientExecutor {

    fun runOnClient(action: () -> Unit) {
        Minecraft.getInstance().execute(action)
    }
}
