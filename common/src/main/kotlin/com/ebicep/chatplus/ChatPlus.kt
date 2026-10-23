package com.ebicep.chatplus

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.features.internal.FeatureManager
import com.ebicep.chatplus.translator.LanguageManager
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

const val MOD_ID = "chatplus"
const val MOD_COLOR = 0xFF12e3DB.toInt()

object ChatPlus {

    val LOGGER: Logger = LogManager.getLogger(MOD_ID)
    var initialized: Boolean = false

    fun init() {
        initialized = true
        LOGGER.info("Initializing ChatPlus")
        LanguageManager
        Config.load()

        Events
        FeatureManager
        LOGGER.info("Done Initializing ChatPlus")
    }

    fun doTest() {
    }

    fun renderTest2() {
//        Minecraft.getInstance().mainRenderTarget.unbindWrite()
//        val nativeImage: NativeImage = Screenshot.takeScreenshot(Minecraft.getInstance().mainRenderTarget)
//        val dynamicTexture = DynamicTexture(nativeImage)
//        dynamicTexture.dumpContents(
//            Identifier.fromNamespaceAndPath(MOD_ID, "test3"),
//            File(Minecraft.getInstance().gameDirectory, Screenshot.SCREENSHOT_DIR).toPath()
//        )
    }

    fun renderTest() {
//        LOGGER.info("Rendering test")
//        val minecraft = Minecraft.getInstance()
//        val TRANSPARENCY_COLOR = Color(54, 57, 63, 255)
//        val width = 1000f
//        val height = 500f
//        RenderSystem.disableDepthTest()
//
////        val renderTarget: RenderTarget = TextureTarget(width.toInt(), height.toInt(), false)
//        renderTarget.setClearColor(TRANSPARENCY_COLOR.red / 255f, TRANSPARENCY_COLOR.green / 255f, TRANSPARENCY_COLOR.blue / 255f, 0f)
////        renderTarget.clear()
//
//        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER_BINDING, renderTarget.frameBufferId)
//
//        renderTarget.bindWrite(true)
////        RenderSystem.setShader(CoreShaders.POSITION_COLOR_TEX_LIGHTMAP)
////        RenderSystem.setShaderTexture(0, renderTarget.colorTextureId)
//        LOGGER.info("Render target: ${renderTarget.frameBufferId}")
//        LOGGER.info("Render target: ${renderTarget.colorTextureId}")
//        LOGGER.info("Render target: ${renderTarget.depthTextureId}")
//
////        RenderSystem.enableBlend()
////        RenderSystem.defaultBlendFunc()
////        RenderSystem.setShader(CoreShaders.POSITION_COLOR_TEX_LIGHTMAP)
////        RenderSystem.setShaderTexture(0, renderTarget.colorTextureId)
////
//
//
////        val window: Window = minecraft.window
////        RenderSystem.clear(256)
////        val matrix4f = (Matrix4f()).setOrtho(
////            0.0f,
////            (width.toDouble() / window.guiScale).toFloat(),
////            (height.toDouble() / window.guiScale).toFloat(),
////            0.0f,
////            1000.0f,
////            21000.0f
////        )
////        RenderSystem.setProjectionMatrix(matrix4f, ProjectionType.ORTHOGRAPHIC)
////        val matrix4fStack = RenderSystem.getModelViewStack()
////        matrix4fStack.pushMatrix()
////        matrix4fStack.translation(0.0f, 0.0f, -11000.0f)
////
////        val guiGraphics = GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource())
////        val poseStack = guiGraphics.pose()
////        poseStack.scale((minecraft.window.guiScaledWidth / width).toFloat(), (minecraft.window.guiScaledHeight / height).toFloat(), 1f)
////        poseStack.pushPose()
////        guiGraphics.fill(0, 0, 100, 100, Color(255, 0, 0, 255).rgb)
////        poseStack.guiForward()
////        guiGraphics.drawString(
////            Minecraft.getInstance().font,
////            "Hello",
////            0,
////            0,
////            0xFFFF00,
////        )
////        guiGraphics.flush()
////        matrix4fStack.popMatrix()
////        RenderSystem.clear(256)
////        poseStack.popPose()
////        val matrix = Matrix4f().setOrtho(0f, width, height, 0f, -1f, 1f)
////        RenderSystem.setProjectionMatrix(matrix, ProjectionType.ORTHOGRAPHIC)
////
////        val tesselator = Tesselator.getInstance()
////        val buffer: BufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)
////        buffer.addVertex(0.0f, 0.0f, 0.0f).setColor(1f, 0f, 0f, 1f)
////        buffer.addVertex(width, 0.0f, 0.0f).setColor(1f, 0f, 0f, 1f)
////        buffer.addVertex(width, height, 0.0f).setColor(1f, 0f, 0f, 1f)
////        buffer.addVertex(0.0f, height, 0.0f).setColor(1f, 0f, 0f, 1f)
////        BufferUploader.drawWithShader(buffer.buildOrThrow())
//
//        renderTarget.unbindWrite()
//        val nativeImage: NativeImage = Screenshot.takeScreenshot(renderTarget)
//        val dynamicTexture = DynamicTexture(nativeImage)
//        dynamicTexture.dumpContents(
//            Identifier.fromNamespaceAndPath(MOD_ID, "test3"),
//            File(Minecraft.getInstance().gameDirectory, Screenshot.SCREENSHOT_DIR).toPath()
//        )
//        renderTarget.destroyBuffers();
//
//        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER_BINDING, minecraft.mainRenderTarget.frameBufferId)
////        val image: Image = getImage(nativeImage)
//        minecraft.mainRenderTarget.bindWrite(false)
    }

    fun isEnabled(): Boolean {
        return Config.values.enabled
    }

    fun sendMessage(component: Component) {
        val message = Component.empty()
            .append(Component.literal("ChatPlus").withColor(MOD_COLOR))
            .append(Component.literal(" > ").withStyle(ChatFormatting.DARK_GRAY))
            .append(component)
        Minecraft.getInstance().execute {
            Minecraft.getInstance().player?.sendSystemMessage(message)
        }
    }

    fun debugLog(message: String) {
        if (Config.values.debugLogging) {
            LOGGER.info(message)
        }
    }

}