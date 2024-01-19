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
import com.ebicep.chatplus.util.GraphicsUtil.guiForward
import com.google.gson.JsonParser
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import org.lwjgl.opengl.GL11
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
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Modified from
 * <a href="https://github.com/comp500/ScreenshotToClipboard">ScreenshotToClipboard</a> and
 * <a href="https://github.com/ramidzkh/fabrishot">fabrishot</a>
 */
object ScreenshotChat {

    const val SCREENSHOT_COLOR = 0xFFFFFFFF

    private const val BYTES_PER_PIXEL = 4
    private var startScreenShot = false
    private var changeBackground = false
    var lastScreenShotTick = -1L

    init {
        EventBus.register<TextBarElements.AddTextBarElementEvent>(6) {
            if (!Config.values.screenShotChatEnabled) {
                return@register
            }
            it.elements.add(ScreenShotChatElement(it.screen))
        }
        EventBus.register<ScreenShotChatEvent> {
            if (!Config.values.screenShotChatEnabled) {
                return@register
            }
            lastScreenShotTick = Events.currentTick
            startScreenShot = true
        }
        EventBus.register<ChatRenderPreLinesEvent> {
            if (startScreenShot) {
                startScreenShot = false
                changeBackground = true
            }
        }
        EventBus.register<ChatRenderLineBackgroundEvent> {
            if (changeBackground) {
                it.backgroundColor = 0xFF36393F.toInt()
            }
        }
        EventBus.register<ChatRenderPostLinesEvent> {
            if (changeBackground) {
                changeBackground = false
                // fill background to change to transparent later
                val guiGraphics = it.guiGraphics
                guiGraphics.pose().guiForward(100.0)
                guiGraphics.fill(
                    ChatRenderer.rescaledX - 10,
                    ChatRenderer.rescaledY - ChatRenderer.rescaledHeight - 10,
                    ChatRenderer.rescaledEndX + 10,
                    ChatRenderer.rescaledY + 10,
                    0xFF36393F.toInt() // 54, 57, 63
                )
                screenshotChat(
                    (min(
                        it.displayMessageIndex,
                        ChatRenderer.rescaledLinesPerPage
                    ) * ChatManager.getLineHeight() * ChatRenderer.scale).roundToInt()
                )
            }
        }
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
        doCopy(array, width, height, components)
    }

    private fun doCopy(imageData: ByteArray, width: Int, height: Int, components: Int) {
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
            upload(bufferedImage)
        }, "Screenshot to Clipboard Copy").start()
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

    private fun screenshotChat(h: Int) {
        val window = Minecraft.getInstance().window

        val guiScale = window.guiScale
        val x = (ChatRenderer.x * guiScale).roundToInt() - 1
        val y = ((window.guiScaledHeight - ChatRenderer.y) * guiScale).roundToInt() - 6
        val width = (ChatRenderer.width * guiScale).roundToInt() + 3
        val height = (h * guiScale).roundToInt() + 2

        val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(width * height * 4)
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1)
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)
        GL11.glReadPixels(x, y, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, byteBuffer)

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

    private fun upload(bufferedImage: BufferedImage?) {
        val baos = ByteArrayOutputStream()
        val exService = Executors.newCachedThreadPool()
        exService.execute {
            Thread.currentThread().name = "Imgur Image Uploading"
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
    }

}