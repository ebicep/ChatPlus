package com.ebicep.chatplus.util

import com.mojang.blaze3d.vertex.PoseStack

object GraphicsUtil {

    inline fun PoseStack.createPose(fn: () -> Unit) {
        pushPose()
        fn()
        popPose()
    }

}