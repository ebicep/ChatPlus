package com.ebicep.chatplus.util

import com.ebicep.chatplus.mixin.IMixinGuiGraphics
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.locale.Language
import net.minecraft.network.chat.FormattedText
import net.minecraft.resources.Identifier
import net.minecraft.util.FormattedCharSequence
import org.joml.Matrix3x2fStack

object GraphicsUtil {

    sealed class GuiForwardType<T>(val amount: Double) {

        open fun getAmount(modifier: () -> T): Double {
            return amount
        }

        data object OnScreenDisplay : GuiForwardType<Unit>(2000.0)
        data object ScreenshotChatLines : GuiForwardType<Unit>(1000.0)
        data object Debug : GuiForwardType<Unit>(500.0)
        data object ChatWindows : GuiForwardType<Int>(100.0) {
            override fun getAmount(modifier: () -> Int): Double {
                return amount * modifier.invoke()
            }
        }

        data object ChatTabNotificationBadge : GuiForwardType<Unit>(60.0)

        data object ChatWindowTab : GuiForwardType<Boolean>(50.0) {
            override fun getAmount(modifier: () -> Boolean): Double { // if global selected tab so renders above other outlines if moved over them
                return if (modifier.invoke()) amount + 2.0 else amount
            }
        }

        data object ChatWindowOutline : GuiForwardType<Boolean>(50.0) {
            override fun getAmount(modifier: () -> Boolean): Double { // if global selected tab so renders above other tab text/outline if moved over them
                return if (modifier.invoke()) amount + 4.0 else amount
            }
        }

        data object ChatRendererDebug : GuiForwardType<Unit>(50.0)
        data object MovableChatMoving : GuiForwardType<Unit>(40.0)
        data object ScreenshotChatFull : GuiForwardType<Unit>(10.0)
        data object MovableChatDebug : GuiForwardType<Unit>(10.0)
        data object Default : GuiForwardType<Boolean>(0.5) {
            override fun getAmount(modifier: () -> Boolean): Double {
                return if (modifier.invoke()) -amount else amount
            }
        }
    }

    inline fun Matrix3x2fStack.createPose(fn: () -> Unit) {
        pushMatrix()
        fn()
        popMatrix()
    }

    fun Matrix3x2fStack.translate0(x: Double = 0.0, y: Double = 0.0) {
        translate(x.toFloat(), y.toFloat())
    }

    fun Matrix3x2fStack.translate0(x: Float = 0f, y: Float = 0f) {
        translate(x, y)
    }

    fun Matrix3x2fStack.translate0(x: Int = 0, y: Int = 0) {
        translate(x.toFloat(), y.toFloat())
    }

    /**
     * Moves the pose stack forward by default 5 (anything new rendered will be on top)
     */
//    fun PoseStack.guiForward(amount: Double = 5.0) {
//        translate(0.0, 0.0, amount / 100)
//    }

    fun <T> PoseStack.guiForward(guiForwardType: GuiForwardType<T>, modifier: () -> T) {
        translate(0.0, 0.0, guiForwardType.getAmount(modifier) / 1000)
    }

    fun PoseStack.guiForward(guiForwardType: GuiForwardType<Unit>) {
        translate(0.0, 0.0, guiForwardType.getAmount {} / 1000)
    }

    fun PoseStack.guiForward(guiForwardType: GuiForwardType<Boolean> = GuiForwardType.Default, backwards: Boolean = false) {
        translate(0.0, 0.0, guiForwardType.getAmount { backwards } / 1000)
    }

    fun GuiGraphics.fill0(i: Float, j: Float, k: Float, l: Float, m: Int) {
        this.fill(RenderPipelines.GUI, i.toInt(), j.toInt(), k.toInt(), l.toInt(), m)
    }

//    fun GuiGraphics.fill0(renderPipeline: RenderPipeline, i: Float, j: Float, k: Float, l: Float, m: Int, n: Int) {
//        var o: Float
//        var i = i
//        var j = j
//        var k = k
//        var l = l
//        if (i < k) {
//            o = i
//            i = k
//            k = o
//        }
//        if (j < l) {
//            o = j
//            j = l
//            l = o
//        }
//
//        this.guiRenderState.submitGuiElement(
//            ColoredRectangleRenderState(
//                renderPipeline,
//                textureSetup,
//                Matrix3x2f(this.pose),
//                i,
//                j,
//                k,
//                l,
//                m,
//                if (integer != null) integer else m,
//                this.scissorStack.peek()
//            )
//        )
//
//
//        val vertexConsumer = this.bufferSource.getBuffer(renderPipeline)
//        vertexConsumer.addVertex(matrix4f, i, j, m.toFloat()).setColor(n).setUv(0f, 0f)
//        vertexConsumer.addVertex(matrix4f, i, l, m.toFloat()).setColor(n).setUv(0f, 0f)
//        vertexConsumer.addVertex(matrix4f, k, l, m.toFloat()).setColor(n).setUv(0f, 0f)
//        vertexConsumer.addVertex(matrix4f, k, j, m.toFloat()).setColor(n).setUv(0f, 0f)
//    }

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
        right: Boolean = true,
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
        right: Boolean = true,
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
        right: Boolean = true,
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
        right: Boolean = true,
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

    fun GuiGraphics.drawString0(string: String, i: Int, j: Int, k: Int) {
        this.drawString(Minecraft.getInstance().font, string, i, j, k, true)
    }

    fun GuiGraphics.drawString0(string: String, x: Float, y: Float, color: Int) {
        this.drawString0(Minecraft.getInstance().font, string, x, y, color, true)
    }

    private fun GuiGraphics.drawString0(font: Font, string: String, x: Float, y: Float, color: Int, bl: Boolean) {
        this.drawString(font, Language.getInstance().getVisualOrder(FormattedText.of(string)), x.toInt(), y.toInt(), color, bl)
    }

    fun GuiGraphics.drawString0(formattedCharSequence: FormattedCharSequence, x: Float, y: Float, color: Int) {
        this.drawString0(formattedCharSequence, x, y, color, true)
    }

    fun GuiGraphics.drawString0(formattedCharSequence: FormattedCharSequence, x: Float, y: Float, color: Int, bl: Boolean) {
        this.drawString(Minecraft.getInstance().font, formattedCharSequence, x.toInt(), y.toInt(), color, bl)
    }

    fun GuiGraphics.blit0(
        renderPipeline: RenderPipeline,
        identifier: Identifier,
        i: Float, // starting x-coordinate for the blit.
        j: Float, // starting y-coordinate for the blit.
        f: Float, // starting u-coordinate in the texture.
        g: Float, // starting v-coordinate in the texture.
        k: Float, // width of the area to be blitted.
        l: Float, // height of the area to be blitted.
        m: Float, // width of the texture.
        n: Float, // height of the texture.
        o: Float, // total width of the texture.
        p: Float, // total height of the texture.
        q: Int, // color to be applied to the vertices.
    ) {
        innerBlit0(
            renderPipeline,
            identifier,
            i,
            i + k,
            j,
            j + l,
            f / o,
            (f + m) / o,
            g / p,
            (g + n) / p,
            q
        )
    }

    private fun GuiGraphics.innerBlit0(
        renderPipeline: RenderPipeline,
        identifier: Identifier,
        i: Float, // starting x-coordinate for the blit.
        j: Float, // ending x-coordinate for the blit.
        k: Float, // starting y-coordinate for the blit.
        l: Float, // ending y-coordinate for the blit.
        f: Float, // starting u-coordinate in the texture.
        g: Float, // ending u-coordinate in the texture.
        h: Float, // starting v-coordinate in the texture.
        m: Float, // ending v-coordinate in the texture.
        n: Int, // color to be applied to the vertices.
    ) {
        this as IMixinGuiGraphics
        this.callInnerBlit(renderPipeline, identifier, i.toInt(), j.toInt(), k.toInt(), l.toInt(), f, g, h, m, n)
    }

    fun GuiGraphics.drawImage(resources: Resources) {
        this.drawImage(resources.identifier, resources.width, resources.height)
    }

    fun GuiGraphics.drawImage(identifier: Identifier, width: Int, height: Int) {
        this.drawImage(identifier, width.toFloat(), height.toFloat())
    }

    fun GuiGraphics.drawImage(identifier: Identifier, width: Float, height: Float) {
        this.innerBlit0(
            RenderPipelines.GUI_TEXTURED,
            identifier,
            0f,
            width,
            0f,
            height,
            0f,
            1f,
            0f,
            1f,
            -1,
        )
    }

    object PlayerHeadUtils {

        fun playerFaceRendererDraw(guiGraphics: GuiGraphics, identifier: Identifier, i: Float, j: Float, k: Float) {
            this.playerFaceRendererDraw(guiGraphics, identifier, i, j, k, renderHat = true, renderUpsideDown = false, l = -1)
        }

        fun playerFaceRendererDraw(
            guiGraphics: GuiGraphics,
            identifier: Identifier,
            i: Float,
            j: Float,
            k: Float,
            renderHat: Boolean,
            renderUpsideDown: Boolean,
            l: Int,
        ) {
            val m = 8 + (if (renderUpsideDown) 8 else 0)
            val n = 8 * (if (renderUpsideDown) -1 else 1)
            guiGraphics.blit0(
                RenderPipelines.GUI_TEXTURED,
                identifier,
                i,
                j,
                8.0f,
                m.toFloat(),
                k,
                k,
                8f,
                n.toFloat(),
                64f,
                64f,
                l
            )
            if (renderHat) {
                playerFaceRendererDrawHat(guiGraphics, identifier, i, j, k, renderUpsideDown, l)
            }
        }

        private fun playerFaceRendererDrawHat(guiGraphics: GuiGraphics, identifier: Identifier, i: Float, j: Float, k: Float, bl: Boolean, l: Int) {
            val m = 8 + (if (bl) 8 else 0)
            val n = 8 * (if (bl) -1 else 1)
            guiGraphics.blit0(
                RenderPipelines.GUI_TEXTURED,
                identifier,
                i,
                j,
                40.0f,
                m.toFloat(),
                k,
                k,
                8f,
                n.toFloat(),
                64f,
                64f,
                l
            )
        }

    }

}