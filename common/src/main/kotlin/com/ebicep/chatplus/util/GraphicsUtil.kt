package com.ebicep.chatplus.util

import com.ebicep.chatplus.mixin.IMixinGuiGraphics
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FastColor
import net.minecraft.util.FormattedCharSequence
import org.joml.Matrix4f

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
     * Moves the pose stack forward by default 5 (anything new rendered will be on top)
     */
    fun PoseStack.guiForward(amount: Double = 5.0) {
        translate(0.0, 0.0, amount / 100)
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
        vertexConsumer.addVertex(matrix4f, i, j, m.toFloat()).setColor(g, h, p, f)
        vertexConsumer.addVertex(matrix4f, i, l, m.toFloat()).setColor(g, h, p, f)
        vertexConsumer.addVertex(matrix4f, k, l, m.toFloat()).setColor(g, h, p, f)
        vertexConsumer.addVertex(matrix4f, k, j, m.toFloat()).setColor(g, h, p, f)
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
        renderOutlineSetPos(startX, startY, startX + width, startY + height, color, thickness, top, bottom, left, right)
    }

    fun GuiGraphics.renderOutlineSetPos(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        color: Int,
        thickness: Float = 1f,
        top: Boolean = true,
        bottom: Boolean = true,
        left: Boolean = true,
        right: Boolean = true
    ) {
        if (top) {
            this.fill0(startX, startY, endX, startY + thickness, color)
        }
        if (bottom) {
            this.fill0(startX, endY - thickness, endX, endY, color)
        }
        if (left) {
            this.fill0(startX, startY + thickness, startX + thickness, endY - thickness, color)
        }
        if (right) {
            this.fill0(endX - thickness, startY + thickness, endX, endY - thickness, color)
        }
    }

    fun GuiGraphics.renderOutline(
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        color: Int,
        thickness: Int = 1,
        top: Boolean = true,
        bottom: Boolean = true,
        left: Boolean = true,
        right: Boolean = true
    ) {
        renderOutlineSetPos(startX, startY, startX + width, startY + height, color, thickness, top, bottom, left, right)
    }

    fun GuiGraphics.renderOutlineSetPos(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        color: Int,
        thickness: Int = 1,
        top: Boolean = true,
        bottom: Boolean = true,
        left: Boolean = true,
        right: Boolean = true
    ) {
        if (top) {
            this.fill(startX, startY, endX, startY + thickness, color)
        }
        if (bottom) {
            this.fill(startX, endY - thickness, endX, endY, color)
        }
        if (left) {
            this.fill(startX, startY + thickness, startX + thickness, endY - thickness, color)
        }
        if (right) {
            this.fill(endX - thickness, startY + thickness, endX, endY - thickness, color)
        }
    }


    fun GuiGraphics.drawHorizontalLine(x1: Int, x2: Int, y: Int, color: Int) {
        this.fill(x1, y, x2, y + 1, color)
    }

    fun GuiGraphics.drawHorizontalLine(x1: Float, x2: Float, y: Float, color: Int, thickness: Float = 1f) {
        this.fill0(x1, y, x2, y + thickness, color)
    }

    fun GuiGraphics.drawHorizontalLine(x1: Int, x2: Int, y: Int, color: Int, thickness: Int = 1) {
        this.fill(x1, y, x2, y + thickness, color)
    }

    fun GuiGraphics.drawVerticalLine(x: Int, y1: Int, y2: Int, color: Int, thickness: Int = 1) {
        this.fill(x, y1, x + thickness, y2, color)
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

    fun playerFaceRendererDraw(guiGraphics: GuiGraphics, resourceLocation: ResourceLocation, i: Float, j: Float, k: Float) {
        this.playerFaceRendererDraw(guiGraphics, resourceLocation, i, j, k, true, false)
    }

    fun playerFaceRendererDraw(guiGraphics: GuiGraphics, resourceLocation: ResourceLocation, i: Float, j: Float, k: Float, bl: Boolean, bl2: Boolean) {
        val l = 8 + (if (bl2) 8 else 0)
        val m = 8 * (if (bl2) -1 else 1)
        guiGraphics.blit0(resourceLocation, i, j, k, k, 8.0f, l.toFloat(), 8f, m.toFloat(), 64f, 64f)
        if (bl) {
            playerFaceRendererDrawHat(guiGraphics, resourceLocation, i, j, k, bl2)
        }
    }

    private fun GuiGraphics.blit0(resourceLocation: ResourceLocation, i: Float, j: Float, k: Float, l: Float, f: Float, g: Float, m: Float, n: Float, o: Float, p: Float) {
        blit0(resourceLocation, i, i + k, j, j + l, 0f, m, n, f, g, o, p)
    }

    private fun GuiGraphics.blit0(
        resourceLocation: ResourceLocation,
        i: Float,
        j: Float,
        k: Float,
        l: Float,
        m: Float,
        n: Float,
        o: Float,
        f: Float,
        g: Float,
        p: Float,
        q: Float
    ) {
        innerBlit0(resourceLocation, i, j, k, l, m, (f + 0.0f) / p, (f + n) / p, (g + 0.0f) / q, (g + o) / q)
    }

    private fun playerFaceRendererDrawHat(guiGraphics: GuiGraphics, resourceLocation: ResourceLocation, i: Float, j: Float, k: Float, bl: Boolean) {
        val l = 8 + (if (bl) 8 else 0)
        val m = 8 * (if (bl) -1 else 1)
        RenderSystem.enableBlend()
        guiGraphics.blit0(resourceLocation, i, j, k, k, 40.0f, l.toFloat(), 8f, m.toFloat(), 64f, 64f)
        RenderSystem.disableBlend()
    }

    private fun GuiGraphics.innerBlit0(resourceLocation: ResourceLocation, i: Float, j: Float, k: Float, l: Float, m: Float, f: Float, g: Float, h: Float, n: Float) {
        RenderSystem.setShaderTexture(0, resourceLocation)
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        val matrix4f: Matrix4f = this.pose().last().pose()
        val bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        bufferBuilder.addVertex(matrix4f, i, k, m).setUv(f, h)
        bufferBuilder.addVertex(matrix4f, i, l, m).setUv(f, n)
        bufferBuilder.addVertex(matrix4f, j, l, m).setUv(g, n)
        bufferBuilder.addVertex(matrix4f, j, k, m).setUv(g, h)
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow())
    }

    fun GuiGraphics.drawImage(resources: Resources) {
        this.innerBlit0(
            resources.resourceLocation,
            0f,
            resources.width.toFloat(),
            0f,
            resources.height.toFloat(),
            0f,
            0f,
            1f,
            0f,
            1f
        )
    }

}