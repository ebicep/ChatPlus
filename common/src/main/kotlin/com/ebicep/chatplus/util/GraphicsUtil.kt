package com.ebicep.chatplus.util

import com.mojang.blaze3d.vertex.PoseStack

object GraphicsUtil {

    inline fun PoseStack.createPose(fn: () -> Unit) {
        pushPose()
        fn()
        popPose()
    }

    fun PoseStack.translateX(x: Float) {
        translate(x, 0f, 0f)
    }

    fun PoseStack.translateY(y: Float) {
        translate(0f, y, 0f)
    }

    fun PoseStack.translateZ(z: Float) {
        translate(0f, 0f, z)
    }

    fun PoseStack.translateX(x: Double) {
        translate(x, 0.0, 0.0)
    }

    fun PoseStack.translateY(y: Double) {
        translate(0.0, y, 0.0)
    }

    fun PoseStack.translateZ(z: Double) {
        translate(0.0, 0.0, z)
    }

    /**
     * Moves the pose stack forward by 50 (anything new rendered will be on top)
     */
    fun PoseStack.guiForward() {
        translate(0.0, 0.0, 50.0)
    }

}