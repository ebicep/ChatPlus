@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package com.ebicep.chatplus.features

import com.ebicep.chatplus.ChatPlus
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
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.drawString0
import com.ebicep.chatplus.util.GraphicsUtil.fill0
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import com.google.gson.JsonParser
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.NativeImage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.minecraft.ChatFormatting
import net.minecraft.Util
import net.minecraft.client.GuiMessage
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
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
import java.net.URL
import java.net.URLEncoder
import java.util.*
import javax.imageio.ImageIO

/**
 * Modified from
 * <a href="https://github.com/comp500/ScreenshotToClipboard">ScreenshotToClipboard</a> and
 * <a href="https://github.com/ramidzkh/fabrishot">fabrishot</a>
 */
object ScreenshotChat {

    val SCREENSHOT_COLOR = -1
    private val TRANSPARENCY_COLOR = Color(54, 57, 63, 255)

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
                screenshot(lastScreenshotSettings!!)
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
        var width = when (screenshotWindowsMode) {
            ScreenshotWindowsMode.STACK -> linesOrdered.maxOf { it.key.renderer.width } / chatRenderer.scale.toFloat()
            ScreenshotWindowsMode.SPLIT -> linesOrdered.map { it.key.renderer.width }.sum() / chatRenderer.scale.toFloat()
        }
        var height = when (screenshotWindowsMode) {
            ScreenshotWindowsMode.STACK -> linesOrdered.map { it.value }.flatten().size * lineHeight
            ScreenshotWindowsMode.SPLIT -> linesOrdered.maxOf { it.value.size } * lineHeight
        }
        val renderTarget: RenderTarget = TextureTarget(width.toInt(), height.toInt(), true, false)
        renderTarget.setClearColor(TRANSPARENCY_COLOR.red / 255f, TRANSPARENCY_COLOR.green / 255f, TRANSPARENCY_COLOR.blue / 255f, 0f)
        renderTarget.clear(false)
        renderTarget.bindWrite(false)
        val minecraft = Minecraft.getInstance()
        val guiGraphics = GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource())
        val poseStack = guiGraphics.pose()
        poseStack.scale((minecraft.window.guiScaledWidth / width).toFloat(), (minecraft.window.guiScaledHeight / height).toFloat(), 1f)
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
        renderTarget.unbindWrite()
        try {
            val nativeImage: NativeImage = Screenshot.takeScreenshot(renderTarget)
            val image: Image = getImage(nativeImage)
            val bufferedImage: BufferedImage = imageToBufferedImage(image)
            ChatPlus.sendMessage(
                Component.literal("Screenshot Taken").withStyle {
                    it.withColor(ChatFormatting.GRAY)
                        .withHoverEvent(
                            HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
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
        } catch (e: Exception) {
            ChatPlus.LOGGER.error(e)
        }
        minecraft.mainRenderTarget.bindWrite(true)
    }

    private fun renderLines(
        chatWindow: ChatWindow,
        guiGraphics: GuiGraphics,
        lines: MutableList<ChatTab.ChatPlusGuiMessageLine>,
        screenshotBackgroundMode: ScreenshotBackgroundMode
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
            var textColor: Int = 0xFFFFFF
            var backgroundColor = if (useChatBackgroundColor) updatedBackgroundColor else 0
            poseStack.createPose {
                poseStack.guiForward()
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
                poseStack.guiForward()
                poseStack.guiForward()
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
        val imageProducer: ImageProducer = ImageIO.read(ByteArrayInputStream(nativeImage.asByteArray())).source
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
                .withStyle { style: Style -> style.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_FILE, newFile.absolutePath)) }
            ChatPlus.sendMessage(Component.translatable("screenshot.success", component).withStyle(ChatFormatting.GRAY))
        } catch (e: IOException) {
            ChatPlus.LOGGER.error(e)
            ChatPlus.sendMessage(Component.translatable("screenshot.failure", e.message).withStyle(ChatFormatting.RED))
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
        if (Minecraft.ON_OSX) {
            return
        }
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
        try {
            val url = URL("https://api.imgur.com/3/image")
            val con = (url.openConnection() as HttpURLConnection).apply {
                doOutput = true
                doInput = true
                requestMethod = "POST"
                setRequestProperty("Authorization", "Client-ID bfea9c11835d95c")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }

            val baos = ByteArrayOutputStream().also {
                ImageIO.write(bufferedImage, "png", it)
            }
            val imageInByte = baos.toByteArray()
            val encoded = Base64.getEncoder().encodeToString(imageInByte)

            val data = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(encoded, "UTF-8")
            OutputStreamWriter(con.outputStream).use { streamWriter ->
                streamWriter.write(data)
                streamWriter.flush()
            }

            val responseCode = con.responseCode
            ChatPlus.LOGGER.info("Response Code: $responseCode")

            BufferedReader(InputStreamReader(con.inputStream)).use { bufferedReader ->
                val stb = StringBuilder()
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    stb.append(line).append("\n")
                }

                val result = JsonParser.parseString(stb.toString()).asJsonObject["data"]
                    .asJsonObject["link"].asString

                // Send result to player
                ChatPlus.sendMessage(
                    Component.literal("Chat Screenshot Link: ").withStyle {
                        it.withColor(ChatFormatting.GRAY)
                    }.append(Component.literal(result).withStyle {
                        it.withColor(ChatFormatting.AQUA)
                            .withUnderlined(true)
                            .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, result))
                            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to open link").withStyle(ChatFormatting.GREEN)))
                    })
                )
            }
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
        val screenshotBackgroundMode: ScreenshotBackgroundMode
    )

}