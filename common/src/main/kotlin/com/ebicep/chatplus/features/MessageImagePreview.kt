package com.ebicep.chatplus.features

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.MOD_ID
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.chattabs.ChatTab
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderPreLineAppearanceEvent
import com.ebicep.chatplus.hud.ChatScreenMouseClickedEvent
import com.ebicep.chatplus.hud.ChatScreenRenderEvent
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.drawImage
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.platform.TextureUtil
import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import javax.imageio.ImageIO

object MessageImagePreview {

    private val FILE_EXTENSIONS: List<String> = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    private val KNOWN_IMAGE_DOMAINS = listOf(
        "unsplash\\.com", "plus\\.unsplash\\.com",
        "imgur\\.com", "i\\.imgur\\.com",
        "giphy\\.com", "media\\d?\\.giphy\\.com",
        "tenor\\.com",
        "cloudinary\\.com",
        "imgix\\.net",
        "cloudfront\\.net",
        "pexels\\.com",
        "flickr\\.com",
        "staticflickr\\.com",
        "500px\\.com",
        "shutterstock\\.com",
        "istockphoto\\.com",
        "gstatic\\.com"
    )
    private val URL_REGEX = Regex(
        "https?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*(),]|%[0-9a-fA-F][0-9a-fA-F])+",
        RegexOption.IGNORE_CASE
    )
    private val IMAGE_PATH_INDICATORS = listOf(
        "/media", "/images", "/photos", "/img", "/picture", "/content", "/cdn", "/upload",
        "/view", "/gif", "/photo", "/premium_photo", "/premium", "/asset", "/static"
    )
    val imageCache = mutableMapOf<String, ChatPlusImage>()
    val lineImages = mutableMapOf<ChatTab.ChatPlusGuiMessage, MutableList<String?>>()

    init {
        EventBus.register<ChatScreenMouseClickedEvent>({ 100 }) {
            if (!Config.values.messageImagePreviewSettings.enabled) {
                return@register
            }
            if (it.button != 1 || !Screen.hasControlDown()) {
                return@register
            }
            ChatManager.globalSelectedTab.getHoveredOverMessageLine(it.mouseX, it.mouseY)?.let { message ->
                val linkedMessage = message.linkedMessage
                val content = ChatFormatting.stripFormatting(linkedMessage.guiMessage.content.string)!!
                val input = content.replace(".", " ").replace(Regex("\\s+"), ".")
                val imageURLs = getAllImageURLs(content)
                if (imageURLs.none()) {
                    ChatPlus.sendMessage(Component.literal("No image URLs found").withStyle {
                        it.withColor(ChatFormatting.GRAY)
                            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(input).withStyle(ChatFormatting.AQUA)))
                    })
                    return@let
                }
                val images = MutableList<String?>(imageURLs.size) { null }
                imageURLs.forEachIndexed { index, url ->
                    try {
                        loadImage(url) { urlString ->
                            if (lineImages[linkedMessage] == null) {
                                lineImages[linkedMessage] = images
                            }
                            images[index] = urlString
                        }
                    } catch (e: Exception) {
                        ChatPlus.LOGGER.error(e)
                        ChatPlus.sendMessage(Component.literal("Failed to load image #$index").withStyle {
                            it.withColor(ChatFormatting.RED)
                                .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(url).withStyle(ChatFormatting.AQUA)))
                        })
                    }
                }
            }
        }
        EventBus.register<ChatScreenRenderEvent> {
            if (!Config.values.messageImagePreviewSettings.enabled) {
                return@register
            }
            ChatManager.globalSelectedTab.getHoveredOverMessageLine()?.let { message ->
                val images = lineImages[message.linkedMessage] ?: return@let
                val guiGraphics = it.guiGraphics
                val poseStack = guiGraphics.pose()
                val chatWindow = ChatManager.selectedWindow
                val renderer = chatWindow.renderer
                val xStart = renderer.internalX
                val xEnd = renderer.backgroundWidthEndX
                val windowWidth = Minecraft.getInstance().window.guiScaledWidth
                val windowHeight = Minecraft.getInstance().window.guiScaledHeight
                val drawRight = windowWidth - xEnd > xStart
                var y = 10f
                images.filterNotNull().forEachIndexed { index, url ->
                    imageCache[url]?.let {
                        poseStack.createPose {
                            fun draw(resourceLocation: ResourceLocation, chatPlusTexture: ChatPlusTexture) {
                                val nativeImage = chatPlusTexture.nativeImage
                                val scale = minOf(windowWidth * .4f / nativeImage.width.toFloat(), windowHeight * .4f / nativeImage.height.toFloat())
                                val width = nativeImage.width * scale
                                val height = nativeImage.height * scale
                                if (drawRight) {
                                    poseStack.translate0(windowWidth - width - 10, y, 10f)
                                } else {
                                    poseStack.translate0(10f, y, 10f)
                                }
                                y += height + 10
//                                guiGraphics.fill(0, 0, width.toInt(), height.toInt(), 0xFF000000.toInt())
                                guiGraphics.drawImage(resourceLocation, width, height)
                            }
                            if (it.maxFrames <= 1) {
                                val resourceLocation = it.resourceLocations[0] ?: return@let
                                val chatPlusTexture = it.chatPlusTextures[0] ?: return@let
                                draw(resourceLocation, chatPlusTexture)
                            } else {
                                val currentFrame = (it.currentFrame % it.maxFrames).toInt()
                                val resourceLocation = it.resourceLocations[currentFrame] ?: return@let
                                val chatPlusTexture = it.chatPlusTextures[currentFrame] ?: return@let
                                draw(resourceLocation, chatPlusTexture)
                                it.currentFrame = (it.currentFrame + .15f) % it.maxFrames
                            }
                        }
                    }

                }
            }
        }
        EventBus.register<ChatRenderPreLineAppearanceEvent>({ Config.values.messageImagePreviewSettings.hasPreviewLinePriority }) {
            if (!Config.values.messageImagePreviewSettings.enabled) {
                return@register
            }
            if (lineImages.containsKey(it.chatPlusGuiMessageLine.linkedMessage)) {
                it.backgroundColor = Config.values.messageImagePreviewSettings.previewLineColor
            }
        }
    }

    fun isLikelyImageUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()

        // check known image domains
        if (KNOWN_IMAGE_DOMAINS.any { domain -> lowerUrl.contains(domain.replace("\\.", ".")) }) {
            return true
        }

        // check common image API patterns
        val apiPatterns = listOf(
            "/image", "getimage", "image=", "photo=", "/avatar", "/thumbnail",
            "ixlib=", "ixid=", "w=", "q=", "auto=format", "fit=crop",
            "gif", "giphy", "tenor"
        )

        if (apiPatterns.any { pattern -> lowerUrl.contains(pattern) }) {
            return true
        }

        // check path indicators
        for (indicator in IMAGE_PATH_INDICATORS) {
            if (lowerUrl.contains(indicator)) {
                return true
            }
        }

        return false
    }

    fun extractUrls(input: String): List<String> {
        return URL_REGEX.findAll(input).map { it.value }.toList()
    }

    fun getAllImageURLs(content: String): List<String> {
        val allUrls = extractUrls(content)
        val imageUrls = mutableListOf<String>()
        for (url in allUrls) {
            val hasImageExtension = FILE_EXTENSIONS.any { ext ->
                url.lowercase().endsWith(".$ext") || url.lowercase().contains(".$ext?")
            }
            if (hasImageExtension || isLikelyImageUrl(url)) {
                imageUrls.add(url)
            }
        }
        return imageUrls.distinct()
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun loadImage(input: String, afterLoad: (String) -> Unit = {}) {
        var validURL = input
        if (!input.startsWith("https://") && !input.startsWith("http://")) {
            validURL = "https://$input"
        }
        val url = URI(validURL).toURL()
        val urlString = url.toString()
        if (urlString in imageCache && imageCache[urlString] != null) {
            return
        }
        ChatPlus.LOGGER.info("Loading Image from URL: $urlString")
        ChatPlus.sendMessage(Component.literal("Loading Image").withStyle {
            it.withColor(ChatFormatting.YELLOW)
                .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(urlString).withStyle(ChatFormatting.AQUA)))
        })
        val chatPlusImage = ChatPlusImage()
        imageCache[urlString] = chatPlusImage
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    setRequestProperty("Accept", "image/*")
                    setRequestProperty("Accept-Encoding", "gzip, deflate")
                    setRequestProperty("Connection", "keep-alive")
                    connectTimeout = 10000
                    readTimeout = 30000
                    useCaches = false
                }
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("HTTP error code: $responseCode")
                }
                val contentType = connection.contentType
                val bytes = connection.inputStream.use { it.readBytes() }
                if (contentType?.lowercase()?.contains("image/gif") == true && validGif(bytes)) {
                    loadAnimatedImage(chatPlusImage, bytes, afterLoad, urlString)
                } else {
                    loadStaticImage(chatPlusImage, bytes, afterLoad, urlString)
                }
            } catch (e: Exception) {
                imageCache.remove(urlString)
                val errorMessage = when (e) {
                    is SocketTimeoutException -> {
                        "Timeout while loading image"
                    }

                    is IOException -> {
                        "Problem retrieving image"
                    }

                    else -> {
                        "Failed to load image"
                    }
                }
                ChatPlus.LOGGER.error("$errorMessage: $urlString", e)
                ChatPlus.sendMessage(Component.literal(errorMessage).withStyle {
                    it.withColor(ChatFormatting.RED)
                        .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(urlString).withStyle(ChatFormatting.AQUA)))
                })
            }
        }
    }

    fun validGif(bytes: ByteArray): Boolean {
        return !(bytes.size < 6 ||
                bytes[0].toInt() != 0x47 || // G
                bytes[1].toInt() != 0x49 || // I
                bytes[2].toInt() != 0x46 || // F
                bytes[3].toInt() != 0x38 || // 8
                (bytes[4].toInt() != 0x37 && bytes[4].toInt() != 0x39) || // 7 or 9
                bytes[5].toInt() != 0x61) // G
    }

    private fun loadStaticImage(
        chatPlusImage: ChatPlusImage,
        bytes: ByteArray,
        afterLoad: (String) -> Unit,
        urlString: String
    ) {
        val bufferedImage = ByteArrayInputStream(bytes).use { stream ->
            ImageIO.read(stream)
        }

        val pngBytes = ByteArrayOutputStream().use { outputStream ->
            ImageIO.write(bufferedImage, "png", outputStream)
            outputStream.toByteArray()
        }

        Minecraft.getInstance().execute {
            loadImage(
                pngBytes,
                "chatplus_message_image_preview",
                { resourceLocation, texture ->
                    chatPlusImage.resourceLocations.add(resourceLocation)
                    chatPlusImage.chatPlusTextures.add(texture)
                },
                afterLoad,
                urlString
            )
            ChatPlus.LOGGER.info("Loaded Image from URL: $urlString")
            ChatPlus.sendMessage(Component.literal("Loaded Image").withStyle {
                it.withColor(ChatFormatting.GREEN)
                    .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(urlString).withStyle(ChatFormatting.AQUA)))
            })
        }
    }

    private fun loadAnimatedImage(
        chatPlusImage: ChatPlusImage,
        bytes: ByteArray,
        afterLoad: (String) -> Unit,
        urlString: String
    ) {
        val gifInputStream = ByteArrayInputStream(bytes).use { stream ->
            ImageIO.createImageInputStream(stream)
        }

        val gifReader = ImageIO.getImageReadersByFormatName("gif").next()
        gifReader.setInput(gifInputStream)

        val totalFrames = gifReader.getNumImages(true)

        if (totalFrames == 0) {
            throw IllegalArgumentException("GIF has no frames")
        }

        chatPlusImage.maxFrames = totalFrames

        Minecraft.getInstance().execute {
            for (currentFrame in 0 until totalFrames) {
                val bufferedImage: BufferedImage = gifReader.read(currentFrame)

                val pngBytes = ByteArrayOutputStream().use { outputStream ->
                    ImageIO.write(bufferedImage, "png", outputStream)
                    outputStream.toByteArray()
                }

                loadImage(
                    pngBytes,
                    "chatplus_message_image_preview_gif_frame",
                    { resourceLocation, texture ->
                        chatPlusImage.resourceLocations.add(resourceLocation)
                        chatPlusImage.chatPlusTextures.add(texture)
                    },
                    afterLoad,
                    urlString
                )
            }
            ChatPlus.LOGGER.info("Loaded GIF from URL: $urlString")
            ChatPlus.sendMessage(Component.literal("Loaded GIF").withStyle {
                it.withColor(ChatFormatting.GREEN)
                    .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(urlString).withStyle(ChatFormatting.AQUA)))
            })
        }
    }

    private fun loadImage(
        pngBytes: ByteArray?,
        prefix: String,
        onResource: (resourceLocation: ResourceLocation, texture: ChatPlusTexture) -> Unit,
        afterLoad: (String) -> Unit,
        urlString: String
    ) {
        try {
            val nativeImage = ByteArrayInputStream(pngBytes).use { inputStream ->
                NativeImage.read(inputStream)
            }

            nativeImage.use { image ->
                val resourceLocation = ResourceLocation(MOD_ID, "${prefix}_${System.currentTimeMillis()}")
                val chatPlusTexture = ChatPlusTexture(image)
                Minecraft.getInstance().textureManager.register(resourceLocation, chatPlusTexture)
                onResource(resourceLocation, chatPlusTexture)

                afterLoad(urlString)
            }
        } catch (e: Exception) {
            ChatPlus.LOGGER.error("Failed to load image", e)
        }
    }

    data class ChatPlusImage(
        var resourceLocations: MutableList<ResourceLocation?> = mutableListOf(),
        var chatPlusTextures: MutableList<ChatPlusTexture?> = mutableListOf(),
        var currentFrame: Float = 1f,
        var maxFrames: Int = 1
    )

    class ChatPlusTexture(val nativeImage: NativeImage) : AbstractTexture() {
        override fun load(resourceManager: ResourceManager) {
            RenderSystem.assertOnRenderThreadOrInit()
            TextureUtil.prepareImage(this.getId(), nativeImage.width, nativeImage.height)
            nativeImage.upload(0, 0, 0, false)
            nativeImage.close()
        }
    }

    @Serializable
    data class MessageImagePreviewSettings(
        var enabled: Boolean = true,
        var hasPreviewLinePriority: Int = 20,
        var previewLineColor: Int = Color(100, 163, 67, 200).rgb,
    )

}