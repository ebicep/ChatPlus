@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package com.ebicep.chatplus.features

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.MOD_ID
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.EnumTranslatableName
import com.ebicep.chatplus.config.MessageDirection
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.features.chatwindows.ChatWindow
import com.ebicep.chatplus.features.chatwindows.RenderWindowsPostEvent
import com.ebicep.chatplus.features.chatwindows.RenderWindowsPreEvent
import com.ebicep.chatplus.features.textbarelements.AddTextBarElementEvent
import com.ebicep.chatplus.features.textbarelements.ScreenShotChatElement
import com.ebicep.chatplus.features.textbarelements.ScreenShotChatEvent
import com.ebicep.chatplus.hud.*
import com.ebicep.chatplus.mixin.IMixinNativeImage
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.drawString0
import com.ebicep.chatplus.util.GraphicsUtil.fill0
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import com.google.gson.JsonParser
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.CommandEncoder
import com.mojang.blaze3d.systems.GpuDevice
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.VertexConsumer
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.minecraft.ChatFormatting
import net.minecraft.client.GuiMessage
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.fog.FogRenderer
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.OutputTarget
import net.minecraft.client.renderer.rendertype.TextureTransform
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.HoverEvent.ShowText
import net.minecraft.network.chat.Style
import net.minecraft.util.Util
import org.lwjgl.stb.STBImage
import java.awt.Color
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.awt.image.FilteredImageSource
import java.awt.image.ImageProducer
import java.awt.image.RGBImageFilter
import java.io.*
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.channels.Channels
import javax.imageio.ImageIO

object ScreenshotChat {

    private fun createRenderType(renderTarget: RenderTarget): RenderType {
        return RenderType.create(
            "$MOD_ID:screenshot",
            RenderSetup.builder(RenderPipelines.TEXT)
                .bufferSize(786432)
                .useLightmap()
                .setOutputTarget(OutputTarget("${MOD_ID}_target") {
                    renderTarget
                })
                .setTextureTransform(TextureTransform.DEFAULT_TEXTURING)
                .setOutline(RenderSetup.OutlineProperty.NONE)
                .createRenderSetup()
        )
    }

    class ChatPlusBufferSource(bufferAllocator: ByteBufferBuilder, renderTarget: RenderTarget) :
        MultiBufferSource.BufferSource(bufferAllocator, Object2ObjectSortedMaps.emptyMap()) {

        private val renderType = createRenderType(renderTarget)
        private var bufferBuilder: BufferBuilder = BufferBuilder(this.sharedBuffer, renderType.mode(), renderType.format())

        override fun getBuffer(renderType: RenderType): VertexConsumer {
            return this.bufferBuilder
        }

        fun endDraw() {
            this.startedBuilders[renderType] = this.bufferBuilder
            endBatch(renderType)
        }

    }

    const val SCREENSHOT_COLOR = -1
    private val TRANSPARENCY_COLOR = Color(54, 57, 63, 255)

    var renderTarget: RenderTarget? = null
    private var takeScreenshot = false
    private var lastScreenShotTick = -1L
    private var linesOrdered = LinkedHashMap<ChatWindow, MutableList<ChatTab.ChatPlusGuiMessageLine>>()
    private var lastScreenshotSettings: ScreenshotSettings? = null

    init {
        var fullScreenshotCounter = -1
        // full chat screenshot
        EventBus.register<AddTextBarElementEvent>({ 150 }) {
            if (!Config.values.screenshotChatEnabled) {
                return@register
            }
            if (!Config.values.screenshotChatTextBarElementEnabled) {
                return@register
            }
            it.elements.add(ScreenShotChatElement(it.screen))
        }
        EventBus.register<ChatScreenRenderEvent> {
            if (takeScreenshot) {
                takeScreenshot = false
                if (lastScreenshotSettings == null) {
                    lastScreenshotSettings = ScreenshotSettings(Config.values.screenshotDefaultScreenShotMode, Config.values.screenshotDefaultScreenBackgroundMode)
                }
                try {
                    screenshot(lastScreenshotSettings!!)
                } catch (e: Exception) {
                    ChatPlus.LOGGER.error(e)
                    ChatPlus.sendMessage(Component.literal("Error Taking Screenshot: " + e.message).withStyle(ChatFormatting.RED))
                }
            }
        }
        EventBus.register<ScreenShotChatEvent> {
            if (!Config.values.screenshotChatEnabled) {
                return@register
            }
            if (ChatManager.globalSelectedTab.displayedMessages.isEmpty()) {
                return@register
            }
            resetScreenShotTick()
            lastScreenshotSettings = it.screenshotSettings
            linesOrdered.clear()
            fullScreenshotCounter = 1
        }
        EventBus.register<RenderWindowsPreEvent> {
            if (fullScreenshotCounter == 1) {
                fullScreenshotCounter = 2
            }
        }
        EventBus.register<ChatRenderLineTextEvent> {
            if (fullScreenshotCounter == 2) {
                linesOrdered.getOrPut(it.chatWindow) { mutableListOf() }.add(it.chatPlusGuiMessageLine)
            }
        }
        EventBus.register<RenderWindowsPostEvent> {
            if (fullScreenshotCounter == 2) {
                // reverse linesOrdered
                linesOrdered = LinkedHashMap<ChatWindow, MutableList<ChatTab.ChatPlusGuiMessageLine>>().apply {
                    linesOrdered.keys.reversed().forEach { put(it, linesOrdered[it]!!) }
                }
                takeScreenshot = true
                fullScreenshotCounter = -1
            }
        }
        var screenshotKeyPressed = false // block other key presses (ctrl s key)
        EventBus.register<ChatScreenInputEvent>({ 1 }, { screenshotKeyPressed }) {
            if (!Config.values.screenshotChatEnabled) {
                return@register
            }
            screenshotKeyPressed = !onCooldown() && Config.values.screenshotChatKey.isDown()
            if (!screenshotKeyPressed) {
                return@register
            }
            val hoveredOverMessage: ChatTab.ChatPlusGuiMessageLine? = ChatManager.globalSelectedTab.getHoveredOverMessageLine()
            if (hoveredOverMessage != null) {
                resetScreenShotTick()
                linesOrdered = LinkedHashMap<ChatWindow, MutableList<ChatTab.ChatPlusGuiMessageLine>>().apply { put(ChatManager.selectedWindow, mutableListOf(hoveredOverMessage)) }
                lastScreenshotSettings = null
                takeScreenshot = true
            } else if (SelectChat.getAllSelectedMessages().isNotEmpty()) {
                resetScreenShotTick()
                linesOrdered = SelectChat.getSelectedMessagesOrderedInWindow()
                lastScreenshotSettings = null
                takeScreenshot = true
            } else {
                EventBus.post(ScreenShotChatEvent())
            }
        }
    }

    fun onCooldown(): Boolean {
        return lastScreenShotTick + 60 > Events.currentTick
    }

    private fun resetScreenShotTick() {
        lastScreenShotTick = Events.currentTick
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun screenshot(screenshotSettings: ScreenshotSettings) {
        val screenshotWindowsMode = Config.values.screenshotDefaultScreenShotWindowsMode
        val screenshotMode = screenshotSettings.screenshotMode
        val screenshotBackgroundMode = screenshotSettings.screenshotBackgroundMode
        if (screenshotMode == ScreenshotMode.CURRENT_WINDOW) {
            linesOrdered.entries.removeIf { it.key != ChatManager.selectedWindow }
        }

        if (linesOrdered.isEmpty()) {
            return
        }

        val chatRenderer = ChatManager.selectedWindow.renderer
        val lineHeight = chatRenderer.lineHeight.toFloat()
        val scale = Config.values.screenshotChatScale
        var width = when (screenshotWindowsMode) {
            ScreenshotWindowsMode.STACK -> linesOrdered.maxOf { it.key.renderer.width } / chatRenderer.scale.toFloat()
            ScreenshotWindowsMode.SPLIT -> linesOrdered.map { it.key.renderer.width }.sum() / chatRenderer.scale.toFloat()
        } * scale
        var height = when (screenshotWindowsMode) {
            ScreenshotWindowsMode.STACK -> linesOrdered.map { it.value }.flatten().size * lineHeight
            ScreenshotWindowsMode.SPLIT -> linesOrdered.maxOf { it.value.size } * lineHeight
        } * scale
        ChatPlus.LOGGER.info("screenshotting = width: $width, height: $height")
        val minecraft = Minecraft.getInstance()
        try {
            renderTarget = TextureTarget("ChatPlusScreenshot", width.toInt(), height.toInt(), true)
            val device: GpuDevice = RenderSystem.getDevice()
            val commandEncoder: CommandEncoder = device.createCommandEncoder()
            val vertexProvider = ChatPlusBufferSource(ByteBufferBuilder(512), renderTarget!!)
            val guiRenderState = GuiRenderState()
            val guiGraphics = GuiGraphics(minecraft, guiRenderState, 0, 0)
            val guiRenderer = GuiRenderer(
                guiRenderState,
                vertexProvider,
                SubmitNodeStorage(),
                minecraft.gameRenderer.featureRenderDispatcher,
                emptyList<PictureInPictureRenderer<*>>()
            )
            val poseStack = guiGraphics.pose()
            commandEncoder.clearColorTexture(renderTarget!!.colorTexture!!, TRANSPARENCY_COLOR.rgb)
            poseStack.scale((minecraft.window.guiScaledWidth / width).toFloat() * scale, (minecraft.window.guiScaledHeight / height).toFloat() * scale)
            when (screenshotWindowsMode) {
                ScreenshotWindowsMode.STACK -> {
                    var h = 0.0
                    linesOrdered.forEach { window, messages ->
                        poseStack.createPose {
                            val renderer = window.renderer
                            poseStack.translate0(x = -renderer.rescaledX.toDouble(), y = -(renderer.rescaledY - messages.size * renderer.lineHeight.toDouble()) + h)
                            renderLines(window, guiGraphics, messages, screenshotBackgroundMode)
                            h += messages.size * renderer.lineHeight.toDouble()
                        }
                    }
                }

                ScreenshotWindowsMode.SPLIT -> {
                    var w = 0.0
                    linesOrdered.forEach { window, messages ->
                        poseStack.createPose {
                            val renderer = window.renderer
                            poseStack.translate0(x = -renderer.rescaledX.toDouble() + w, y = -(renderer.rescaledY - messages.size * renderer.lineHeight.toDouble()))
                            renderLines(window, guiGraphics, messages, screenshotBackgroundMode)
                            w += window.renderer.rescaledWidth
                        }
                    }
                }
            }
            guiRenderer.render(minecraft.gameRenderer.fogRenderer.getBuffer(FogRenderer.FogMode.NONE))
            vertexProvider.endDraw()
            try {
                Screenshot.takeScreenshot(renderTarget!!) { nativeImage ->
                    val image: Image = getImage(nativeImage)
                    val bufferedImage: BufferedImage = imageToBufferedImage(image)
                    ChatPlus.sendMessage(
                        Component.literal("Screenshot Taken").withStyle {
                            it.withColor(ChatFormatting.GRAY)
                                .withHoverEvent(
                                    ShowText(
                                        Component.literal("Dimensions: ").withStyle(ChatFormatting.GRAY)
                                            .append(Component.literal("${width.toInt()} x ${height.toInt()}").withStyle(ChatFormatting.AQUA))
                                            .append(Component.literal("\nWindow Mode: ").withStyle(ChatFormatting.GRAY))
                                            .append(Component.translatable(screenshotWindowsMode.key).withStyle(ChatFormatting.GREEN))
                                            .append(Component.literal("\nMode: ").withStyle(ChatFormatting.GRAY))
                                            .append(Component.translatable(screenshotMode.key).withStyle(ChatFormatting.GREEN))
                                            .append(Component.literal("\nBackground Mode: ").withStyle(ChatFormatting.GRAY))
                                            .append(Component.translatable(screenshotBackgroundMode.key).withStyle(ChatFormatting.GREEN))
                                    )
                                )
                        })
                    if (Config.values.screenshotChatSaveToFile) {
                        GlobalScope.launch(Dispatchers.IO) {
                            saveToFile(bufferedImage)
                        }
                    }
                    if (Config.values.screenshotChatCopyToClipboard) {
                        GlobalScope.launch(Dispatchers.IO) {
                            copy(bufferedImage)
                        }
                    }
                    if (Config.values.screenshotChatAutoUpload) {
                        GlobalScope.launch(Dispatchers.IO) {
                            upload(bufferedImage)
                        }
                    }
                }
            } catch (e: Exception) {
                ChatPlus.LOGGER.error("Error taking screenshot", e)
            }
            guiRenderer.close()
        } catch (e: Exception) {
            ChatPlus.LOGGER.error("Error preparing screenshot", e)
        } finally {
            renderTarget = null
        }
    }

    private fun renderLines(
        chatWindow: ChatWindow,
        guiGraphics: GuiGraphics,
        lines: MutableList<ChatTab.ChatPlusGuiMessageLine>,
        screenshotBackgroundMode: ScreenshotBackgroundMode,
    ) {
        val renderer = chatWindow.renderer
        val poseStack = guiGraphics.pose()
        val updatedBackgroundColor = chatWindow.generalSettings.getUpdatedBackgroundColor()
        val useChatBackgroundColor =
            screenshotBackgroundMode == ScreenshotBackgroundMode.KEEP_BACKGROUND || screenshotBackgroundMode == ScreenshotBackgroundMode.KEEP_BACKGROUND_SHOW_LINE_COLOR
        lines.forEachIndexed { displayMessageIndex: Int, chatPlusGuiMessageLine: ChatTab.ChatPlusGuiMessageLine ->
            val messageIndex = chatWindow.tabSettings.selectedTab.displayedMessages.indexOf(chatPlusGuiMessageLine)
            val line: GuiMessage.Line = chatPlusGuiMessageLine.line
            // how high chat is from input bar, if changed need to change queue offset
            val verticalChatOffset: Float = when (chatWindow.generalSettings.messageDirection) {
                MessageDirection.TOP_DOWN -> (renderer.rescaledY - renderer.rescaledLinesPerPage * renderer.lineHeight + renderer.lineHeight) + displayMessageIndex * renderer.lineHeight
                MessageDirection.BOTTOM_UP -> renderer.rescaledY - displayMessageIndex * renderer.lineHeight
            }
            val verticalTextOffset: Float = verticalChatOffset + renderer.l1 // align text with background
            var textColor = -1
            var backgroundColor = if (useChatBackgroundColor) updatedBackgroundColor else 0
            poseStack.createPose {
                val lineAppearanceEvent = ChatRenderPreLineAppearanceEvent(
                    guiGraphics,
                    chatWindow,
                    chatPlusGuiMessageLine,
                    verticalChatOffset,
                    verticalTextOffset,
                    textColor,
                    backgroundColor
                )
                EventBus.post(lineAppearanceEvent)
                textColor = lineAppearanceEvent.textColor
                if (screenshotBackgroundMode == ScreenshotBackgroundMode.KEEP_BACKGROUND_SHOW_LINE_COLOR || screenshotBackgroundMode == ScreenshotBackgroundMode.SHOW_LINE_COLOR) {
                    backgroundColor = lineAppearanceEvent.backgroundColor
                }
                //background
                guiGraphics.fill0(
                    renderer.internalX / renderer.scale,
                    verticalChatOffset - renderer.lineHeight.toFloat(),
                    renderer.rescaledEndX,
                    verticalChatOffset,
                    backgroundColor
                )
            }
            poseStack.createPose {
                EventBus.post(
                    ChatRenderLineTextEvent(
                        guiGraphics,
                        chatWindow,
                        chatPlusGuiMessageLine,
                        1.0,
                        255,
                        backgroundColor,
                        verticalChatOffset,
                        verticalTextOffset,
                        chatPlusGuiMessageLine.content,
                        messageIndex
                    )
                )
                // text
                guiGraphics.drawString0(
                    line.content,
                    renderer.rescaledX,
                    verticalTextOffset,
                    textColor
                )
            }
        }
    }

    private fun getImage(nativeImage: NativeImage): Image {
        val imageProducer: ImageProducer = ImageIO.read(ByteArrayInputStream(asByteArray(nativeImage))).source
        return Toolkit.getDefaultToolkit().createImage(
            FilteredImageSource(
                imageProducer,
                object : RGBImageFilter() {
                    override fun filterRGB(x: Int, y: Int, rgb: Int): Int {
                        if (rgb or 0xFF000000.toInt() == (TRANSPARENCY_COLOR.rgb or 0xFF000000.toInt())) {
                            return 0x00FFFFFF and rgb
                        }
                        return rgb
                    }
                }
            )
        )
    }

    @Throws(IOException::class)
    fun asByteArray(nativeImage: NativeImage): ByteArray {
        nativeImage as IMixinNativeImage
        ByteArrayOutputStream().use { byteArrayOutputStream ->
            Channels.newChannel(byteArrayOutputStream).use { writableByteChannel ->
                if (!nativeImage.callWriteToChannel(writableByteChannel)) {
                    throw IOException("Could not write image to byte array: ${STBImage.stbi_failure_reason()}")
                }
            }
            return byteArrayOutputStream.toByteArray()
        }
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

    private fun imageToBufferedImage(image: Image): BufferedImage {
        val bufferedImage = BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB)
        val graphics = bufferedImage.createGraphics()
        graphics.drawImage(image, 0, 0, null)
        graphics.dispose()
        return bufferedImage
    }

    private fun saveToFile(bufferedImage: BufferedImage) {
        try {
            val file = File(Minecraft.getInstance().gameDirectory, Screenshot.SCREENSHOT_DIR)
            file.mkdir()
            val newFile = getFile(file)
            ImageIO.write(bufferedImage, "png", newFile)
            val component = Component.literal(newFile.name)
                .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                .withStyle { style: Style -> style.withClickEvent(ClickEvent.OpenFile(newFile.absolutePath)) }
            ChatPlus.sendMessage(Component.translatable("screenshot.success", component).withStyle(ChatFormatting.GRAY))
        } catch (e: IOException) {
            ChatPlus.LOGGER.error(e)
            ChatPlus.sendMessage(Component.translatable("screenshot.failure", e.message ?: "Unknown error").withStyle(ChatFormatting.RED))
        }
    }

    private fun getFile(file: File): File {
        val string = Util.getFilenameFormattedDateTime()
        var i = 1
        var file2: File
        while ((File(file, string + (if (i == 1) "" else "_$i") + ".png").also { file2 = it }).exists()) {
            ++i
        }
        return file2
    }

    private fun copy(bufferedImage: BufferedImage) {
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val transferable = getTransferableImage(bufferedImage)
            clipboard.setContents(transferable, null)
        } catch (e: Exception) {
            ChatPlus.LOGGER.error(e)
            ChatPlus.sendMessage(Component.literal("Error Copying Screenshot to Clipboard").withStyle(ChatFormatting.RED))
        }
    }

    private fun upload(bufferedImage: BufferedImage?) {
        if (Config.values.screenshotChatAutoUploadSettings.secret.isEmpty()) {
            ChatPlus.sendMessage(Component.literal("Unable to upload screenshot, no secret provided.").withStyle(ChatFormatting.RED))
            return
        }
        try {
            val boundary = "----WebKitFormBoundary" + System.currentTimeMillis()
            val url = URL("https://api.imgur.com/3/image")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                doOutput = true
                doInput = true
                requestMethod = "POST"
                if (Config.values.screenshotChatAutoUploadSettings.anonymousUpload) {
                    setRequestProperty("Authorization", "Client-ID ${Config.values.screenshotChatAutoUploadSettings.secret}")
                } else {
                    setRequestProperty("Authorization", "Bearer ${Config.values.screenshotChatAutoUploadSettings.secret}")
                }
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            val baos = ByteArrayOutputStream().also {
                ImageIO.write(bufferedImage, "png", it)
            }

            DataOutputStream(connection.outputStream).apply {
                writeBytes("--$boundary\r\n")
                writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"image.png\"\r\n")
                writeBytes("Content-Type: image/png\r\n")
                writeBytes("Content-Transfer-Encoding: binary\r\n\r\n")
                write(baos.toByteArray())
                writeBytes("\r\n--$boundary--\r\n")
                // outputStream.writeBytes("Content-Disposition: form-data; name=\"album\"\r\n\r\n")
                // outputStream.writeBytes(albumId)
                // outputStream.writeBytes("\r\n--$boundary--\r\n")
                flush()
            }

            val response = connection.inputStream.bufferedReader().readText()
            ChatPlus.LOGGER.info("Response: $response")

            val result = JsonParser.parseString(response).asJsonObject["data"].asJsonObject["link"].asString

            // Send result to player
            ChatPlus.sendMessage(
                Component.literal("Chat Screenshot Link: ").withStyle {
                    it.withColor(ChatFormatting.GRAY)
                }.append(Component.literal(result).withStyle {
                    it.withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(ClickEvent.OpenUrl(URI(result)))
                        .withHoverEvent(HoverEvent.ShowText(Component.literal("Click to open link").withStyle(ChatFormatting.GREEN)))
                })
            )
        } catch (e: Exception) {
            ChatPlus.LOGGER.error(e)
            ChatPlus.sendMessage(Component.literal("Error Uploading Screenshot").withStyle(ChatFormatting.RED))
        }
    }

    @Serializable
    enum class ScreenshotMode(val key: String) : EnumTranslatableName {
        CURRENT_WINDOW("chatPlus.screenshotScreenShotWindowsMode.current"),
        ALL_WINDOWS("chatPlus.screenshotScreenShotWindowsMode.all"),

        ;

        val translatable: Component = Component.translatable(key)

        override fun getTranslatableName(): Component {
            return translatable
        }
    }

    @Serializable
    enum class ScreenshotBackgroundMode(val key: String) : EnumTranslatableName {
        KEEP_BACKGROUND("chatPlus.screenshotBackgroundMode.keepBackground"),
        KEEP_BACKGROUND_SHOW_LINE_COLOR("chatPlus.screenshotBackgroundMode.keepBackgroundShowLineColor"),
        TRANSPARENT("chatPlus.screenshotBackgroundMode.transparent"),
        SHOW_LINE_COLOR("chatPlus.screenshotBackgroundMode.showLineColor"), // line colors from highlight/select/etc

        ;

        val translatable: Component = Component.translatable(key)

        override fun getTranslatableName(): Component {
            return translatable
        }
    }

    @Serializable
    enum class ScreenshotWindowsMode(val key: String) : EnumTranslatableName {
        STACK("chatPlus.screenshotScreenShotWindowsMode.stack"),
        SPLIT("chatPlus.screenshotScreenShotWindowsMode.split"),

        ;

        val translatable: Component = Component.translatable(key)

        override fun getTranslatableName(): Component {
            return translatable
        }
    }

    data class ScreenshotSettings(
        val screenshotMode: ScreenshotMode,
        val screenshotBackgroundMode: ScreenshotBackgroundMode,
    )

    @Serializable
    data class ScreenshotUploadSettings(
        var anonymousUpload: Boolean = false,
        var secret: String = "",
    )

}


////            vertexProvider.finish()
//
////            meshData.close()
//
////            val vertexProvider = OverrideVertexProvider(ByteBufferBuilder(256))
////            val vertexConsumer = vertexProvider.getBuffer(RENDER_TYPE) as BufferBuilder
////            val guiGraphics = GuiGraphics(minecraft, vertexProvider)
////            val poseStack = guiGraphics.pose()
//
////            poseStack.scale(10f, 10f, 1f)
//
////            val images = listOf(
////                ResourceLocation.withDefaultNamespace("skins/3b0c6e9a8d249bac856a3ec261fe5d528431e0a7"),
////                ResourceLocation.withDefaultNamespace("skins/3b0c6e9a8d249bac856a3ec261fe5d528431e0a7"),
////                ResourceLocation.withDefaultNamespace("skins/3b0c6e9a8d249bac856a3ec261fe5d528431e0a7")
////            )
////
////            data class DrawEntry(
////                val texture: AbstractTexture,
////                val vertexBuffer: GpuBuffer,
////                val indexBuffer: GpuBuffer,
////                val indexType: VertexFormat.IndexType,
////                val indexCount: Int,
////            )
////
////            ChatPlus.LOGGER.info("preparing")
////
////            val drawEntries = mutableListOf<DrawEntry>()
////            val vertexProvider = OverrideVertexProvider(ByteBufferBuilder(256))
////            val bufferBuilder = vertexProvider.getBuffer(RENDER_TYPE) as BufferBuilder
////            val guiGraphics = GuiGraphics(minecraft, vertexProvider)
////            val poseStack = guiGraphics.pose()
////            poseStack.scale(.10f, .10f, 1f)
////            var index = 0
////            for (resourceLocation in images) {
////                ChatPlus.LOGGER.info("loading texture $resourceLocation")
////                poseStack.createPose {
////                    GraphicsUtil.PlayerHeadUtils.playerFaceRendererDraw(guiGraphics, resourceLocation, index * 200f, index * 200f, width)
////                }
////                index++
////                ChatPlus.LOGGER.info("done loading texture $resourceLocation")
////            }
////            guiGraphics.drawString0(Minecraft.getInstance().font, "HELLO WORLD", 0f, 0f, Color(255, 0, 0, 255).rgb)
//////            when (screenshotWindowsMode) {
//////                ScreenshotWindowsMode.STACK -> {
//////                    var h = 0.0
//////                    linesOrdered.forEach { window, messages ->
//////                        poseStack.createPose {
//////                            val renderer = window.renderer
//////                            poseStack.translate0(x = -renderer.rescaledX.toDouble(), y = -(renderer.rescaledY - messages.size * renderer.lineHeight.toDouble()) + h)
//////                            renderLines(window, guiGraphics, messages, screenshotBackgroundMode)
//////                            h += messages.size * renderer.lineHeight.toDouble()
//////                        }
//////                    }
//////                }
//////
//////                ScreenshotWindowsMode.SPLIT -> {
//////                    var w = 0.0
//////                    linesOrdered.forEach { window, messages ->
//////                        poseStack.createPose {
//////                            val renderer = window.renderer
//////                            poseStack.translate0(x = -renderer.rescaledX.toDouble() + w, y = -(renderer.rescaledY - messages.size * renderer.lineHeight.toDouble()))
//////                            renderLines(window, guiGraphics, messages, screenshotBackgroundMode)
//////                            w += window.renderer.rescaledWidth
//////                        }
//////                    }
//////                }
//////            }
////            guiGraphics.flush()
////            RENDER_TYPE.setupRenderState()
//////            var index = 0
//////            for (resourceLocation in images) {
//////                ChatPlus.LOGGER.info("loading texture $resourceLocation")
//////                GraphicsUtil.PlayerHeadUtils.playerFaceRendererDraw(guiGraphics, resourceLocation, index * 200f, index * 200f, width)
//////                guiGraphics.flush()
//////
//////                val meshData = vertexProvider.bufferBuilder.build()!!
//////                val vertexBuffer = PIPELINE.vertexFormat.uploadImmediateVertexBuffer(meshData.vertexBuffer())
//////                val indexBuffer = meshData.indexBuffer()
//////                val (gpuIndexBuffer, indexType) = if (indexBuffer == null) {
//////                    val auto = RenderSystem.getSequentialBuffer(meshData.drawState().mode())
//////                    auto.getBuffer(meshData.drawState().indexCount()) to auto.type()
//////                } else {
//////                    PIPELINE.vertexFormat.uploadImmediateIndexBuffer(indexBuffer) to meshData.drawState().indexType()
//////                }
//////
//////                val texture = Minecraft.getInstance().textureManager.getTexture(resourceLocation)
//////                drawEntries += DrawEntry(texture, vertexBuffer, gpuIndexBuffer, indexType, meshData.drawState().indexCount())
//////                meshData.close()
//////
//////                index++
//////                ChatPlus.LOGGER.info("done loading texture $resourceLocation")
//////            }
////
////            ChatPlus.LOGGER.info("starting render pass")
////
////            val meshData = vertexProvider.bufferBuilder.buildOrThrow()!!
////            val gpuBuffer = PIPELINE.vertexFormat.uploadImmediateVertexBuffer(meshData.vertexBuffer())
////
////            val textureManager = minecraft.textureManager
////            val indexBuffer = meshData.indexBuffer()
////
////            val gpuIndexBuffer: GpuBuffer
////            val indexType: VertexFormat.IndexType
////
////            if (indexBuffer == null) {
////                val autoIndex = RenderSystem.getSequentialBuffer(meshData.drawState().mode())
////                gpuIndexBuffer = autoIndex.getBuffer(meshData.drawState().indexCount())
////                indexType = autoIndex.type()
////            } else {
////                gpuIndexBuffer = PIPELINE.vertexFormat.uploadImmediateIndexBuffer(indexBuffer)
////                indexType = meshData.drawState().indexType()
////            }
////            RenderSystem.getDevice()
////                .createCommandEncoder()
////                .createRenderPass(
////                    renderTarget.colorTexture,
////                    OptionalInt.of(TRANSPARENCY_COLOR.rgb),
////                    null,
////                    OptionalDouble.empty()
////                )
////                .use { renderPass ->
////                    renderPass.setPipeline(PIPELINE)
////                    renderPass.setVertexBuffer(0, gpuBuffer)
////
////                    var indexOffset = 0
////                    // Bind textures used (you may need to bind multiple if different)
////                    for (resourceLocation in images) {
////                        ChatPlus.LOGGER.info("binding texture ${resourceLocation}")
////                        val texture = textureManager.getTexture(resourceLocation)
////                        renderPass.bindSampler("Sampler0", texture.getTexture()) // if all use same sampler
////                        // Calculate how many indices this texture group uses
////                        val indicesForThisTexture = images.size * 6 // 6 indices per quad
////                        renderPass.setIndexBuffer(gpuIndexBuffer, indexType)
////                        renderPass.drawIndexed(indexOffset, indicesForThisTexture)
////                        indexOffset += indicesForThisTexture
////                    }
////                }
////
////            meshData.close()
////            RENDER_TYPE.clearRenderState()