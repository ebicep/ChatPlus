package com.ebicep.chatplus.util

import com.mojang.blaze3d.vertex.PoseStack

object GraphicsUtil {

    inline fun PoseStack.createPose(fn: () -> Unit) {
        pushPose()
        fn()
        popPose()
    }

    /**
     * Moves the pose stack forward by 50 (anything new rendered will be on top)
     */
    fun PoseStack.guiForward() {
        translate(0.0, 0.0, 50.0)
    }

}