package com.ebicep.chatplus.util

import com.ebicep.chatplus.mixin.IMixinGuiGraphics
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import net.minecraft.util.FastColor
import net.minecraft.util.FormattedCharSequence

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

    fun GuiGraphics.fill0(i: Float, j: Float, k: Float, l: Float, n: Int) {
        this.fill0(RenderType.gui(), i, j, k, l, 0, n)
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
        vertexConsumer.vertex(matrix4f, i, j, m.toFloat()).color(g, h, p, f).endVertex()
        vertexConsumer.vertex(matrix4f, i, l, m.toFloat()).color(g, h, p, f).endVertex()
        vertexConsumer.vertex(matrix4f, k, l, m.toFloat()).color(g, h, p, f).endVertex()
        vertexConsumer.vertex(matrix4f, k, j, m.toFloat()).color(g, h, p, f).endVertex()
        this.callFlushIfUnmanaged()
    }

    fun GuiGraphics.renderOutline(
        startX: Float,
        startY: Float,
        width: Float,
        height: Float,
        color: Int,
        thickness: Float = 1f,
        top: Boolean = true,
        bottom: Boolean = true,
        left: Boolean = true,
        right: Boolean = true
    ) {
        if (top) {
            this.fill0(startX, startY, startX + width, startY + thickness, color)
        }
        if (bottom) {
            this.fill0(startX, startY + height - thickness, startX + width, startY + height, color)
        }
        if (left) {
            this.fill0(startX, startY + thickness, startX + thickness, startY + height - thickness, color)
        }
        if (right) {
            this.fill0(startX + width - thickness, startY + thickness, startX + width, startY + height - thickness, color)
        }
    }

    fun GuiGraphics.drawHorizontalLine(x1: Int, x2: Int, y: Int, color: Int) {
        this.fill(x1, y, x2, y + 1, color)
    }

    fun GuiGraphics.drawHorizontalLine(x1: Float, x2: Float, y: Float, color: Int, thickness: Float = 1f) {
        this.fill0(x1, y, x2, y + thickness, color)
    }

    fun GuiGraphics.drawString0(font: Font, string: String, x: Float, y: Float, color: Int): Int {
        return this.drawString0(font, string, x, y, color, true)
    }

    fun GuiGraphics.drawString0(font: Font, string: String, x: Float, y: Float, color: Int, bl: Boolean): Int {
        this as IMixinGuiGraphics
        val l = font.drawInBatch(
            string,
            x,
            y,
            color,
            bl,
            this.pose().last().pose(),
            this.bufferSource(),
            Font.DisplayMode.NORMAL,
            0,
            0xF000F0,
            font.isBidirectional
        )
        this.callFlushIfUnmanaged()
        return l
    }

    fun GuiGraphics.drawString0(font: Font, formattedCharSequence: FormattedCharSequence, x: Float, y: Float, color: Int): Int {
        return this.drawString0(font, formattedCharSequence, x, y, color, true)
    }

    fun GuiGraphics.drawString0(font: Font, formattedCharSequence: FormattedCharSequence, x: Float, y: Float, color: Int, bl: Boolean): Int {
        this as IMixinGuiGraphics
        val l = font.drawInBatch(
            formattedCharSequence,
            x,
            y,
            color,
            bl,
            this.pose().last().pose(),
            this.bufferSource(),
            Font.DisplayMode.NORMAL,
            0,
            0xF000F0
        )
        this.callFlushIfUnmanaged()
        return l
    }

}