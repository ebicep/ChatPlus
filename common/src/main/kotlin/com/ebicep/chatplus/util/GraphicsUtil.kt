package com.ebicep.chatplus.util

import com.ebicep.chatplus.mixin.IMixinGuiGraphics
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import net.minecraft.util.FastColor

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

    fun GuiGraphics.fill0(i: Float, j: Float, k: Float, l: Float, m: Int, n: Int) {
        this.fill0(RenderType.gui(), i, j, k, l, m, n)
    }

    fun GuiGraphics.fill0(renderType: RenderType, i: Float, j: Float, k: Float, l: Float, m: Int, n: Int) {
        this as IMixinGuiGraphics
        val matrix4f = this.pose().last().pose()
        var o: Float
        var i = i
        var j = j
        var k = k
        var l = l
        if (i < k) {
            o = i
            i = k
            k = o
        }
        if (j < l) {
            o = j
            j = l
            l = o
        }
        val f = FastColor.ARGB32.alpha(n) / 255.0f
        val g = FastColor.ARGB32.red(n) / 255.0f
        val h = FastColor.ARGB32.green(n) / 255.0f
        val p = FastColor.ARGB32.blue(n) / 255.0f
        val vertexConsumer = this.bufferSource.getBuffer(renderType)
        vertexConsumer.addVertex(matrix4f, i, j, m.toFloat()).setColor(g, h, p, f)
        vertexConsumer.addVertex(matrix4f, i, l, m.toFloat()).setColor(g, h, p, f)
        vertexConsumer.addVertex(matrix4f, k, l, m.toFloat()).setColor(g, h, p, f)
        vertexConsumer.addVertex(matrix4f, k, j, m.toFloat()).setColor(g, h, p, f)
        this.`chatPlus$flushIfUnmanaged`()
    }

}