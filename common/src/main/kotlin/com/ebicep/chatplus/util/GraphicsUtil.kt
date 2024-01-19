package com.ebicep.chatplus.util

import com.mojang.blaze3d.vertex.PoseStack

object GraphicsUtil {

    inline fun PoseStack.createPose(fn: () -> Unit) {
        pushPose()
        fn()
        popPose()
    }

    fun PoseStack.translate0(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0) {
        translate(x, y, z)
    }

    fun PoseStack.translate0(x: Float = 0f, y: Float = 0f, z: Float = 0f) {
        translate(x, y, z)
    }

    fun PoseStack.translate0(x: Int = 0, y: Int = 0, z: Int = 0) {
        translate(x.toDouble(), y.toDouble(), z.toDouble())
    }

    /**
     * Moves the pose stack forward by default 50 (anything new rendered will be on top)
     */
    fun PoseStack.guiForward(amount: Double = 50.0) {
        translate(0.0, 0.0, amount)
    }

}