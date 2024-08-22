@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package com.ebicep.chatplus.features

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.textbarelements.ScreenShotChatElement
import com.ebicep.chatplus.features.textbarelements.ScreenShotChatEvent
import com.ebicep.chatplus.features.textbarelements.TextBarElements
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import com.ebicep.chatplus.util.TimeStampedLines
import com.google.gson.JsonParser
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.vertex.*
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.util.Mth
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.awt.Toolkit
import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.*
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Modified from
 * <a href="https://github.com/comp500/ScreenshotToClipboard">ScreenshotToClipboard</a> and
 * <a href="https://github.com/ramidzkh/fabrishot">fabrishot</a>
 */
object ScreenshotChat {

    val SCREENSHOT_COLOR = -1
    private val SCREENSHOT_TRANSPARENCY_COLOR = Color(54, 57, 63, 255).rgb
    private const val PADDING = 5
    private const val BYTES_PER_PIXEL = 4

    private var screenshotMode: ScreenshotMode = ScreenshotMode.NONE
    private var lastLinesScreenShotted: TimeStampedLines? = null
    private var lastScreenShotTick = -1L

    enum class ScreenshotMode {
        NONE, FULL, SELECTED, LINE
    }

    init {
        // full chat screenshot
        EventBus.register<TextBarElements.AddTextBarElementEvent>({ 150 }) {
            if (!Config.values.screenshotChatEnabled) {
                return@register
            }
            if (!Config.values.screenshotChatTextBarElementEnabled) {
                return@register
            }
            it.elements.add(ScreenShotChatElement(it.screen))
        }
        EventBus.register<ScreenShotChatEvent> {
            if (!Config.values.screenshotChatEnabled) {
                return@register
            }
            if (ChatManager.globalSelectedTab.displayedMessages.isEmpty()) {
                return@register
            }
            resetScreenShotTick()
            screenshotMode = ScreenshotMode.FULL
        }
        EventBus.register<ChatRenderPostLinesEvent> {
            if (screenshotMode != ScreenshotMode.FULL) {
                return@register
            }
            screenshotMode = ScreenshotMode.NONE
            val chatWindow = ChatManager.selectedWindow
            val renderer = chatWindow.renderer
            // fill background to change to transparent later
            val guiGraphics = it.guiGraphics
            guiGraphics.pose().guiForward(100.0)
            guiGraphics.fill(
                renderer.rescaledX - 10,
                renderer.rescaledY - renderer.rescaledHeight - 10,
                renderer.rescaledEndX + 10,
                renderer.rescaledY + 10,
                SCREENSHOT_TRANSPARENCY_COLOR
            )
            screenshotUnscaledPadded(
                renderer.internalX.toDouble(),
                renderer.internalY.toDouble() + 2,
                renderer.internalWidth.toDouble(),
                min(
                    it.displayMessageIndex,
                    renderer.rescaledLinesPerPage
                ) * 9 * renderer.scale.toDouble() + 2
            )
        }
        // line screenshot
        var lineScreenShotted = false
        EventBus.register<ChatScreenKeyPressedEvent>({ 1 }, { lineScreenShotted }) {
            if (!Config.values.screenshotChatEnabled) {
                return@register
            }
            lineScreenShotted = !onCooldown() && Config.values.screenshotChatLine.isDown()
            if (!lineScreenShotted) {
                return@register
            }
            val hoveredOverMessage = ChatManager.globalSelectedTab.getHoveredOverMessageLine()
            if (hoveredOverMessage != null) {
                resetScreenShotTick()
                screenshotMode = ScreenshotMode.LINE
                lastLinesScreenShotted = TimeStampedLines(hoveredOverMessage, Events.currentTick + 60)
            } else if (SelectChat.getAllSelectedMessages().isNotEmpty()) {
                resetScreenShotTick()
                screenshotMode = ScreenshotMode.SELECTED
                lastLinesScreenShotted = TimeStampedLines(SelectChat.getSelectedMessagesOrdered().toMutableList(), Events.currentTick + 60)
            } else {
                EventBus.post(ScreenShotChatEvent())
            }
            it.returnFunction = true
        }
        var preventOtherRendering = false
        EventBus.register<ChatRenderPreLineAppearanceEvent>({ Config.values.screenshotChatLinePriority }, { preventOtherRendering }) {
            if (lastLinesScreenShotted?.matches(it.line) == true) {
                preventOtherRendering = true
                it.backgroundColor = SCREENSHOT_COLOR
            } else {
                preventOtherRendering = false
            }
        }
        EventBus.register<ChatRenderPreLinesEvent> {
            if (lastLinesScreenShotted == null) {
                return@register
            }
            if (screenshotMode != ScreenshotMode.SELECTED && screenshotMode != ScreenshotMode.LINE) {
                return@register
            }
            screenshotMode = ScreenshotMode.NONE
            val chatWindow = ChatManager.selectedWindow
            val renderer = chatWindow.renderer
            val guiGraphics = it.guiGraphics
            val poseStack = guiGraphics.pose()
            val line = lastLinesScreenShotted!!.lines
            poseStack.createPose {
                poseStack.guiForward(1000.0)
                poseStack.scale(renderer.scale, renderer.scale, 1f)
                guiGraphics.fill(
                    0,
                    0,
                    renderer.rescaledWidth + 10,
                    20 * line.size,
                    SCREENSHOT_TRANSPARENCY_COLOR
                )
                val linesOrdered = if (line.size == 1) {
                    mutableListOf(line.first())
                } else {
                    SelectChat.getSelectedMessagesOrdered()
                }
                linesOrdered.forEach { line ->
                    guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        line.line.content,
                        PADDING,
                        20 / 3,
                        0xFFFFFF
                    )
                    poseStack.translate0(y = 10.0)
                }
                screenshotUnscaledPadded(
                    0.0,
                    16.0 * renderer.scale + ((line.size - 1) * 10.0 * renderer.scale),
                    renderer.internalWidth + PADDING.toDouble() * renderer.scale,
                    10.0 * renderer.scale * line.size
                )
            }
        }
    }

    fun onCooldown(): Boolean {
        return lastScreenShotTick + 60 > Events.currentTick
    }

    private fun resetScreenShotTick() {
        lastScreenShotTick = Events.currentTick
    }

    private fun screenshot(renderer: ChatRenderer, y: Int, h: Int) {
        val chatWindow = Minecraft.getInstance().window
        val guiScale = chatWindow.guiScale

        screenshot(
            (renderer.internalX * guiScale).roundToInt() - 1,
            ((chatWindow.guiScaledHeight - y) * guiScale).roundToInt() - 6,
            (renderer.internalWidth * guiScale).roundToInt() + 3,
            (h * guiScale).roundToInt() + 2
        )
    }

    private fun screenshot(x: Double, y: Double, width: Double, height: Double) {
        screenshot(x.roundToInt(), y.roundToInt(), width.roundToInt(), height.roundToInt())
    }

    private fun screenshotUnscaledPadded(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        lateralPadding: Double = 1.5,
        verticalPadding: Double = 0.5,
    ) {
        val chatWindow = Minecraft.getInstance().window
        val guiScale = chatWindow.guiScale
        val xPadded = x - lateralPadding
        val widthPadded = width + verticalPadding * 2
        val heightPadded = height + verticalPadding * 2
        screenshot(
            xPadded * guiScale,
            chatWindow.height - y * guiScale,
            widthPadded * guiScale,
            heightPadded * guiScale
        )
    }

    private fun screenshot(x: Int, y: Int, width: Int, height: Int) {
//        ChatPlus.LOGGER.info("Screenshotting $x, $y, $width, $height")
        val chatWindow = Minecraft.getInstance().window
        val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(width * height * 4)
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1)
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)
        GL11.glReadPixels(
            Mth.clamp(x, 0, chatWindow.width),
            Mth.clamp(y, 0, chatWindow.height),
            Mth.clamp(width, 0, chatWindow.width - x),
            Mth.clamp(height, 0, chatWindow.height - y),
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            byteBuffer
        )

        // Iterate through the ByteBuffer to add alpha channel
        for (i in 0 until width * height) {
            val baseIndex = i * BYTES_PER_PIXEL // Each pixel has 4 channels (RGBA)

            val red = byteBuffer.get(baseIndex).toInt() and 0xFF
            val green = byteBuffer.get(baseIndex + 1).toInt() and 0xFF
            val blue = byteBuffer.get(baseIndex + 2).toInt() and 0xFF
            val alpha = if (red == 54 && green == 57 && blue == 63) 0 else 255

            byteBuffer.put(baseIndex, red.toByte())     // Red
            byteBuffer.put(baseIndex + 1, green.toByte()) // Green
            byteBuffer.put(baseIndex + 2, blue.toByte())  // Blue
            byteBuffer.put(baseIndex + 3, alpha.toByte()) // Alpha
        }

        val line1 = ByteArray(width * BYTES_PER_PIXEL)
        val line2 = ByteArray(width * BYTES_PER_PIXEL)

        // flip buffer vertically
        for (i in 0 until height / 2) {
            val ofs1: Int = i * width * BYTES_PER_PIXEL
            val ofs2: Int = (height - i - 1) * width * BYTES_PER_PIXEL

            // read lines
            byteBuffer.position(ofs1)
            byteBuffer[line1]
            byteBuffer.position(ofs2)
            byteBuffer[line2]

            // write lines at swapped positions
            byteBuffer.position(ofs2)
            byteBuffer.put(line1)
            byteBuffer.position(ofs1)
            byteBuffer.put(line2)
        }
        byteBuffer.rewind()

        handleScreenshotAWT(byteBuffer, width, height, 4)
    }

    private fun handleScreenshotAWT(byteBuffer: ByteBuffer, width: Int, height: Int, components: Int) {
        if (Minecraft.ON_OSX) {
            return
        }
        val array: ByteArray
        if (byteBuffer.hasArray()) {
            array = byteBuffer.array()
        } else {
            // can't use .array() as the buffer is not array-backed
            array = ByteArray(height * width * components)
            byteBuffer[array]
        }
        copyToClipboardAndUpload(array, width, height, components)
    }

    private fun copyToClipboardAndUpload(imageData: ByteArray, width: Int, height: Int, components: Int) {
        Thread({
            val buf = DataBufferByte(imageData, imageData.size)
            val cs: ColorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB)
            val nBits = intArrayOf(8, 8, 8, 8)
            val bOffs = intArrayOf(0, 1, 2, 3)
            val colorModel: ColorModel = ComponentColorModel(
                cs,
                nBits,
                true,
                false,
                Transparency.TRANSLUCENT,
                DataBuffer.TYPE_BYTE
            )
            val bufferedImage = BufferedImage(
                colorModel,
                Raster.createInterleavedRaster(
                    buf,
                    width,
                    height,
                    width * components,
                    components,
                    bOffs,
                    null
                ),
                false,
                null
            )
            val transferable = getTransferableImage(bufferedImage)
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(transferable, null)
            ChatPlus.sendMessage(Component.literal("Screenshot Taken").withStyle { it.withColor(ChatFormatting.GRAY) })
            if (Config.values.screenshotChatAutoUpload) {
                upload(bufferedImage)
            }
        }, "Copy ScreenShot + Upload").start()
    }

    private fun getTransferableImage(bufferedImage: BufferedImage): Transferable {
        return object : Transferable {
            override fun getTransferDataFlavors(): Array<DataFlavor> {
                return arrayOf(DataFlavor.imageFlavor)
            }

            override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
                return DataFlavor.imageFlavor.equals(flavor)
            }

            @Throws(UnsupportedFlavorException::class)
            override fun getTransferData(flavor: DataFlavor): Any {
                if (DataFlavor.imageFlavor.equals(flavor)) {
                    return bufferedImage
                }
                throw UnsupportedFlavorException(flavor)
            }
        }
    }

    private fun upload(bufferedImage: BufferedImage?) {
        val baos = ByteArrayOutputStream()
        val responseCode: Int
        try {
            val url = URL("https://api.imgur.com/3/image")
            val con = url.openConnection() as HttpURLConnection
            con.doOutput = true
            con.doInput = true
            con.requestMethod = "POST"
            con.setRequestProperty("Authorization", "Client-ID bfea9c11835d95c")
            con.requestMethod = "POST"
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            con.connect()
            ImageIO.write(bufferedImage, "png", baos)
            baos.flush()
            val imageInByte = baos.toByteArray()
            val encoded = Base64.getEncoder().encodeToString(imageInByte)
            val streamWriter = OutputStreamWriter(con.outputStream)
            val data = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(encoded, "UTF-8")
            streamWriter.write(data)
            streamWriter.flush()
            val bufferedReader = BufferedReader(InputStreamReader(con.inputStream))
            var line: String?
            val stb = StringBuilder()
            while (bufferedReader.readLine().also { line = it } != null) {
                stb.append(line).append("\n")
            }
            // Get the response
            responseCode = con.responseCode
            ChatPlus.LOGGER.info("Response Code: $responseCode")
            streamWriter.close()
            bufferedReader.close()
            val jsonObject = JsonParser.parseString(stb.toString()).asJsonObject
            val result = jsonObject["data"].asJsonObject["link"].asString

            //Send result to player
            ChatPlus.sendMessage(
                Component.literal("Chat Screenshot Link: ").withStyle {
                    it.withColor(ChatFormatting.GRAY)
                }.append(Component.literal(result).withStyle {
                    it.withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, result))
                        .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to open link")))
                })
            )
        } catch (e: Exception) {
            ChatPlus.LOGGER.error("ERROR UPLOADING SCREENSHOT")
            ChatPlus.LOGGER.error(e)
        }
    }

    object ScreenshotRenderTarget : RenderTarget(false) {
//
//        val FULL: Int = (255 shl 24) or
//                (0 shl 16) or
//                (255 shl 8) or
//                (0)
//
//        init {
//            width = Minecraft.getInstance().window.width
//            height = Minecraft.getInstance().window.height
//            RenderSystem.assertOnRenderThreadOrInit()
//            val dimension = allocateAttachments(width, height)
//            frameBufferId = GlStateManager.glGenFramebuffers()
//            GlStateManager._glBindFramebuffer(36160, frameBufferId)
//            GlStateManager._bindTexture(colorTextureId)
//            GlStateManager._texParameter(3553, 10241, 9728)
//            GlStateManager._texParameter(3553, 10240, 9728)
//            GlStateManager._texParameter(3553, 10242, 33071)
//            GlStateManager._texParameter(3553, 10243, 33071)
//            GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, colorTextureId, 0)
//            GlStateManager._bindTexture(depthBufferId)
//            GlStateManager._texParameter(3553, 34892, 0)
//            GlStateManager._texParameter(3553, 10241, 9728)
//            GlStateManager._texParameter(3553, 10240, 9728)
//            GlStateManager._texParameter(3553, 10242, 33071)
//            GlStateManager._texParameter(3553, 10243, 33071)
//            GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, depthBufferId, 0)
//            GlStateManager._bindTexture(0)
//            viewWidth = dimension!!.width
//            viewHeight = dimension.height
//            this.width = dimension.width
//            this.height = dimension.height
//            checkStatus()
//            GlStateManager._glBindFramebuffer(36160, 0)
//        }
//
//        private fun allocateAttachments(i: Int, j: Int): Dimension? {
//            RenderSystem.assertOnRenderThreadOrInit()
//            colorTextureId = TextureUtil.generateTextureId()
//            depthBufferId = TextureUtil.generateTextureId()
//            var attachmentState = AttachmentState.NONE
//            val var4: Iterator<*> = Dimension.listWithFallback(i, j).iterator()
//            var dimension: Dimension?
//            do {
//                if (!var4.hasNext()) {
//                    throw RuntimeException("Unrecoverable GL_OUT_OF_MEMORY (allocated attachments = " + attachmentState.name + ")")
//                }
//                dimension = var4.next() as Dimension
//                attachmentState = AttachmentState.NONE
//                if (this.allocateColorAttachment(dimension)) {
//                    attachmentState = attachmentState.with(AttachmentState.COLOR)
//                }
//                if (this.allocateDepthAttachment(dimension)) {
//                    attachmentState = attachmentState.with(AttachmentState.DEPTH)
//                }
//            } while (attachmentState != AttachmentState.COLOR_DEPTH)
//            return dimension
//        }
//
//        private fun allocateColorAttachment(arg: Dimension): Boolean {
//            RenderSystem.assertOnRenderThreadOrInit()
//            GlStateManager._getError()
//            GlStateManager._bindTexture(colorTextureId)
//            GlStateManager._texImage2D(3553, 0, 32856, arg.width, arg.height, 0, 6408, 5121, null as IntBuffer?)
//            return GlStateManager._getError() != 1285
//        }
//
//        private fun allocateDepthAttachment(arg: Dimension): Boolean {
//            RenderSystem.assertOnRenderThreadOrInit()
//            GlStateManager._getError()
//            GlStateManager._bindTexture(depthBufferId)
//            GlStateManager._texImage2D(3553, 0, 6402, arg.width, arg.height, 0, 6402, 5126, null as IntBuffer?)
//            return GlStateManager._getError() != 1285
//        }
//
//
//        private class Dimension internal constructor(val width: Int, val height: Int) {
//            override fun equals(other: Any?): Boolean {
//                return if (this === other) {
//                    true
//                } else if (other != null && this.javaClass == other.javaClass) {
//                    val dimension = other as Dimension
//                    width == dimension.width && height == dimension.height
//                } else {
//                    false
//                }
//            }
//
//            override fun hashCode(): Int {
//                return Objects.hash(*arrayOf<Any>(width, height))
//            }
//
//            override fun toString(): String {
//                return width.toString() + "x" + height
//            }
//
//            companion object {
//                private val DEFAULT_DIMENSIONS = Dimension(854, 480)
//
//                fun listWithFallback(i: Int, j: Int): List<Dimension> {
//                    RenderSystem.assertOnRenderThreadOrInit()
//                    val k = RenderSystem.maxSupportedTextureSize()
//                    return if (i > 0 && i <= k && j > 0 && j <= k) ImmutableList.of(
//                        Dimension(i, j),
//                        DEFAULT_DIMENSIONS
//                    ) else ImmutableList.of(
//                        DEFAULT_DIMENSIONS
//                    )
//                }
//            }
//        }
//
//        private enum class AttachmentState {
//            NONE, COLOR, DEPTH, COLOR_DEPTH;
//
//            fun with(arg: AttachmentState): AttachmentState {
//                return values()[ordinal or arg.ordinal]
//            }
//        }
//
//        fun ss() {
////            blitToScreen(width, height)
////            createFrameBuffer()
//            thingy(width, height, false)
//        }
//
//        private fun createFrameBuffer() {
//            RenderSystem.assertOnRenderThreadOrInit()
//            clear(false)
//            GL11.glMatrixMode(GL11.GL_PROJECTION)
//            GL11.glLoadIdentity()
//            GL11.glOrtho(0.0, width.toDouble(), height.toDouble(), 0.0, 1000.0, 3000.0)
//            GL11.glMatrixMode(GL11.GL_MODELVIEW)
//            GL11.glLoadIdentity()
//            GL11.glTranslatef(0.0f, 0.0f, -2000.0f)
//            bindWrite(true)
//        }
//
//        private fun screenshot() {
////            val i = w * h
////            val pixelBuffer = BufferUtils.createIntBuffer(i)
////            val pixelValues = IntArray(i)
////            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1)
////            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)
//
//            renderer.render(GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource()), 0, 0, 0)
//
//            bindRead()
////            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelBuffer)
////            pixelBuffer.get(pixelValues)
////            TextureUtil.prepareImage()
//            screenshot(0, 0, width, height)
//        }
//
//        private fun thingy2() {
//            val minecraft = Minecraft.getInstance()
//            val window: Window = minecraft.getWindow()
//            RenderSystem.clear(256, Minecraft.ON_OSX)
//            val matrix4f = Matrix4f().setOrtho(
//                0.0f,
//                (window.width.toDouble() / window.guiScale).toFloat(),
//                (window.height.toDouble() / window.guiScale).toFloat(),
//                0.0f,
//                1000.0f,
//                21000.0f
//            )
//            RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z)
//            val poseStack = RenderSystem.getModelViewStack()
//            poseStack.pushPose()
//            poseStack.setIdentity()
//            poseStack.translate(0.0f, 0.0f, -11000.0f)
//            RenderSystem.applyModelViewMatrix()
//            Lighting.setupFor3DItems()
//            val guiGraphics = GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource())
//
//            RenderSystem.enableBlend()
//
//            poseStack.createPose {
//                poseStack.translate(50f, 50f, -1000000000f)
//                poseStack.scale(2f, 2f, 0f)
//                guiGraphics.drawString(Minecraft.getInstance().font, "dwajiohawdhui42uhiouhicuiho412uhi", 0, 0, 0xFFFFFF)
//            }
//
//
//            RenderSystem.clear(256, Minecraft.ON_OSX)
//
//            guiGraphics.flush()
//            poseStack.popPose()
//            RenderSystem.applyModelViewMatrix()
//        }
//
//        private fun thingy(w: Int, h: Int, bl: Boolean) {
//            RenderSystem.assertOnRenderThread()
//            GlStateManager._colorMask(true, true, true, false)
//            GlStateManager._disableDepthTest()
//            GlStateManager._depthMask(false)
//            GlStateManager._viewport(0, 0, w, h)
//            if (bl) {
//                GlStateManager._disableBlend()
//            }
//            val minecraft = Minecraft.getInstance()
//            val shaderInstance = minecraft.gameRenderer.blitShader
//            shaderInstance.setSampler("DiffuseSampler", colorTextureId)
//            val matrix4f = Matrix4f().setOrtho(0.0f, w.toFloat(), h.toFloat(), 0.0f, 1000.0f, 3000.0f)
//            RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z)
//            if (shaderInstance.MODEL_VIEW_MATRIX != null) {
//                shaderInstance.MODEL_VIEW_MATRIX!!.set(Matrix4f().translation(0.0f, 0.0f, -2000.0f))
//            }
//            if (shaderInstance.PROJECTION_MATRIX != null) {
//                shaderInstance.PROJECTION_MATRIX!!.set(matrix4f)
//            }
//            shaderInstance.apply()
//            val width = w.toFloat()
//            val height = h.toFloat()
//            val widthScale = viewWidth.toFloat() / this.width.toFloat()
//            val heightScale = viewHeight.toFloat() / this.height.toFloat()
//
//            val tesselator = RenderSystem.renderThreadTesselator()
//            val bufferBuilder: BufferBuilder = tesselator.builder
////            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)
////            bufferBuilder.vertex(0.0, height.toDouble(), 0.0)
////                .uv(0.0f, 0.0f)
////                .color(255, 255, 255, 255)
////                .endVertex()
////            bufferBuilder.vertex(width.toDouble(), height.toDouble(), 0.0)
////                .uv(widthScale, 0.0f)
////                .color(255, 255, 255, 255)
////                .endVertex()
////            bufferBuilder.vertex(width.toDouble(), 0.0, 0.0)
////                .uv(widthScale, heightScale)
////                .color(255, 255, 255, 255)
////                .endVertex()
////            bufferBuilder.vertex(0.0, 0.0, 0.0)
////                .uv(0.0f, heightScale)
////                .color(255, 255, 255, 255)
////                .endVertex()
////            BufferUploader.draw(bufferBuilder.end())
//
//            Minecraft.getInstance().font.drawInBatch(
//                "HELLO",
//                0f,
//                0f,
//                FULL,
//                false,
//                matrix4f,
//                Minecraft.getInstance().renderBuffers().bufferSource(),
//                Font.DisplayMode.NORMAL,
//                0,
//                LightTexture.FULL_BRIGHT
//            )
//            GlStateManager.
////            val guiGraphics = GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource())
////            val poseStack = guiGraphics.pose()
////            poseStack.createPose {
////                poseStack.translate(50f, 50f, -100000f)
////                poseStack.scale(10f, 10f, 0f)
////                guiGraphics.drawString(Minecraft.getInstance().font, "HELLO", 50, 50, 0xFFFFFF)
////            }
////            guiGraphics.flush()
//
////            renderer.render(GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource()), 0, 0, 0)
//
////            screenshot()
//
//            shaderInstance.clear()
//            GlStateManager._depthMask(true)
//            GlStateManager._colorMask(true, true, true, true)
//
//            bindWrite(true)
//        }
//
//    }
    }
}