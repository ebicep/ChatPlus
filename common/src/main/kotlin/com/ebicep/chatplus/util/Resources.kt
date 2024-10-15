package com.ebicep.chatplus.util

import com.ebicep.chatplus.MOD_ID
import net.minecraft.resources.ResourceLocation

enum class Resources(path: String, var width: Int, var height: Int) {

    NOTIFICATION_BADGE("notification_badge.png", 16, 16)

    ;

    var resourceLocation: ResourceLocation = ResourceLocation(MOD_ID, path)

}