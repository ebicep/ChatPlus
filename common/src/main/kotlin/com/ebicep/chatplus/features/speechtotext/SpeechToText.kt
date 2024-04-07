package com.ebicep.chatplus.features.speechtotext

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.configDirectoryPath
import com.ebicep.chatplus.events.ChatPlusTickEvent
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.ChatPlusScreen
import com.ebicep.chatplus.hud.ChatScreenCloseEvent
import com.ebicep.chatplus.hud.ChatScreenInitPreEvent
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import com.ebicep.chatplus.util.KeyUtil.isDown
import com.google.gson.JsonParser
import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientGuiEvent
import dev.architectury.event.events.client.ClientRawInputEvent
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.lwjgl.openal.ALC11
import org.vosk.Model
import org.vosk.Recognizer
import java.awt.Color
import java.io.File
import java.util.regex.Pattern

val SAMPLE_RATE: Int
    get() = Config.values.speechToTextSampleRate

class MicrophoneException(override val message: String) : Exception(message)

object SpeechToText {

    private val DEVICE_NAME = Pattern.compile("^(?:OpenAL.+?on )?(.*)$")

    @Volatile
    var recordMic = false
    val microphoneThread = MicrophoneThread()
    private var alFailed = false

    init {
        microphoneThread.start()
        EventBus.register<ChatPlusTickEvent> {
            recordMic = Config.values.speechToTextMicrophoneKey.isDown() && Minecraft.getInstance().screen !is ChatPlusScreen
        }
        ClientGuiEvent.RENDER_HUD.register { guiGraphics, tickDelta ->

        }
    }

    fun getAllPossibleModels(): MutableList<String> {
        val modelDirectory = File("$configDirectoryPath\\models")
        if (!modelDirectory.exists()) {
            modelDirectory.mkdir()
        }
        // get all folders
        val files: Array<File> = modelDirectory.listFiles { file -> file.isDirectory } ?: arrayOf()
        return files.map { it.name }.toMutableList()
    }

    fun createMicrophone(): Microphone {
        return try {
            val mic = Config.values.speechToTextMicrophone
            val device = if (mic == "" || mic == "Default") null else mic
            ChatPlus.LOGGER.info("Microphone: $device")
            ALMicrophone(SAMPLE_RATE, SAMPLE_RATE / 1000 * 20, device)
        } catch (e: MicrophoneException) {
            ChatPlus.LOGGER.error(e)
            alFailed = true
            JavaxMicrophone(SAMPLE_RATE, null)
        }
    }

    fun getAllMicrophoneNames(): MutableList<String> {
        val names: MutableList<String> = mutableListOf()
        (if (alFailed) {
            JavaxMicrophone.getMicrophoneNames()
        } else {
            ALMicrophone.getMicrophoneNames()
        }).forEach {
            val matcher = DEVICE_NAME.matcher(it)
            names.add(if (!matcher.matches()) it else matcher.group(1))
        }
        return names
    }

    fun canEnumerate(): Boolean {
        val present = ALC11.alcIsExtensionPresent(0L, "ALC_ENUMERATE_ALL_EXT")
        checkALCError(0L)
        return present
    }
}

class MicrophoneThread : Thread("ChatPlusMicrophoneThread") {

    private val LISTENING_COLOR = Color(0, 200, 0, 255).rgb
    private val FAILED_COLOR = Color(200, 0, 0, 255).rgb
    private var listening = false
    private var running = true
    private var disabled = false
    private var microphone: Microphone? = null
    private var recognizer: Recognizer? = null
    private var lastSpokenMessage: String? = null
    private var totalData: MutableList<Short> = mutableListOf()
    private var quickSendTimer: Long = 0 // -1 = quicked sended, 0 = idle
    private val canQuickSend: Boolean
        get() = quickSendTimer > System.currentTimeMillis() && lastSpokenMessage != null

    init {
        ChatPlus.LOGGER.info("SpeechToText initialized")
        EventBus.register<ChatScreenInitPreEvent> {
            SpeechToText.recordMic = false
            lastSpokenMessage.let { message ->
                if (message != null && quickSendTimer > 0) {
                    it.screen.initial = message
                }
            }
        }
        EventBus.register<ChatScreenCloseEvent> {
            lastSpokenMessage = null
        }
        ClientGuiEvent.RENDER_HUD.register { guiGraphics, tickDelta ->
            if (listening) {
                val centerWidth = Minecraft.getInstance().window.guiScaledWidth / 2
                val text = "Listening"
                val width = Minecraft.getInstance().font.width(text)
                guiGraphics.fill(
                    centerWidth - width / 2 - 5,
                    35,
                    centerWidth + width / 2 + 5,
                    53,
                    2130706432
                )
                guiGraphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    text,
                    centerWidth,
                    40,
                    LISTENING_COLOR
                )
            }
            if (canQuickSend) {
                val failed = lastSpokenMessage.isNullOrEmpty()
                val centerWidth = Minecraft.getInstance().window.guiScaledWidth / 2
                val text = if (failed) "Failed" else lastSpokenMessage!!
                val width = Minecraft.getInstance().font.width(text)
                val poseStack = guiGraphics.pose()
                poseStack.createPose {
                    guiGraphics.fill(
                        centerWidth - width / 2 - 5,
                        35,
                        centerWidth + width / 2 + 5,
                        53,
                        2130706432
                    )
                    guiGraphics.drawCenteredString(
                        Minecraft.getInstance().font,
                        text,
                        centerWidth,
                        40,
                        if (failed) FAILED_COLOR else -1
                    )
                }
                if (!failed) {
                    poseStack.createPose {
                        val scale = .8f
                        poseStack.scale(scale, scale, scale)
                        poseStack.translate0(x = centerWidth / scale, y = 55 / scale)
                        guiGraphics.drawCenteredString(
                            Minecraft.getInstance().font,
                            Component.literal("Quick Send (")
                                .append(Config.values.speechToTextQuickSendKey.displayName)
                                .append(Component.literal(")")),
                            0,
                            0,
                            LISTENING_COLOR
                        )
                    }
                }
            }
        }
        ClientRawInputEvent.KEY_PRESSED.register { client, keyCode, scanCode, action, modifiers ->
            val quickSend = keyCode == Config.values.speechToTextQuickSendKey.value && Minecraft.getInstance().screen !is ChatPlusScreen
            if (canQuickSend && quickSend) {
                quickSendTimer = -1
                lastSpokenMessage?.let { Minecraft.getInstance().player?.connection?.sendChat(it) }
            }
            EventResult.pass()
        }
    }

    fun resetRecognizer() {
        disabled = false
        recognizer = null
    }

    fun resetMicrophone() {
        disabled = false
        microphone = null
    }

    override fun run() {
        while (running) {
            if (disabled) {
                sleep(10000)
                continue
            }
            if (recognizer == null) {
                try {
                    recognizer = Recognizer(
                        Model("$configDirectoryPath\\models\\" + Config.values.speechToTextSelectedAudioModel),
                        SAMPLE_RATE.toFloat()
                    )
                    val recognizedModel = "Recognized Model: ${Config.values.speechToTextSelectedAudioModel}"
                    ChatPlus.LOGGER.info(recognizedModel)
                    ChatPlus.sendMessage(Component.literal(recognizedModel).withStyle {
                        it.withColor(ChatFormatting.GREEN)
                    })
                } catch (e: Exception) {
                    ChatPlus.sendMessage(Component.literal("Failed to load model, disabling Speech to Text.").withStyle {
                        it.withColor(ChatFormatting.RED)
                    })
                    ChatPlus.LOGGER.error(e)
                    disabled = true
                }
            }
            try {
                val recordMic = SpeechToText.recordMic
                if (recordMic && !listening) {
                    quickSendTimer = 0
                    totalData.clear()
                    val mic = getMicrophone()
                    if (mic == null) {
                        throw MicrophoneException("Failed to get microphone")
                    }
                    ChatPlus.LOGGER.info("Started Recording")
                    mic.startRecording()
                    listening = true
                } else if (!recordMic && listening) {
                    ChatPlus.LOGGER.info("Done Recording")
                    listening = false
                    val mic = getMicrophone()
                    if (mic == null) {
                        throw MicrophoneException("Failed to get microphone")
                    }
                    totalData.addAll(readMic().toList())
                    ChatPlus.LOGGER.info("Data: ${totalData.size}")
                    mic.stopRecording()
                    speechToText(totalData.toShortArray())
                    lastSpokenMessage = JsonParser.parseString(recognizer!!.finalResult).asJsonObject.get("text")?.asString
                    quickSendTimer = System.currentTimeMillis() + 3000
                    ChatPlus.LOGGER.info("Final: $lastSpokenMessage")
                    if (lastSpokenMessage.isNullOrBlank()) {
                        continue
                    }
                    val screen = Minecraft.getInstance().screen
                    if (screen is ChatPlusScreen) {
                        lastSpokenMessage?.let { screen.input?.insertText(it) }
                    }
                } else if (listening) {
                    ChatPlus.LOGGER.debug("available: ${microphone!!.dataAvailable()}")
                    totalData.addAll(readMic().toList())
                    sleep()
                }
            } catch (e: Exception) {
                ChatPlus.LOGGER.error(e)
                disabled = true
            }
        }
    }

    private fun sleep() {
        try {
            sleep(100)
        } catch (e: InterruptedException) {
            ChatPlus.LOGGER.error(e)
        }
    }

    private fun readMic(): ShortArray {
        val mic = getMicrophone()
        if (mic == null) {
            sleep(10000)
            throw MicrophoneException("Failed to get microphone")
        }
        if (!mic.isActive()) {
            mic.startRecording()
        }
        return mic.read()
    }

    private fun speechToText(data: ShortArray): String? {
        return if (recognizer!!.acceptWaveForm(data, data.size)) {
            JsonParser.parseString(recognizer!!.result).asJsonObject.get("text")?.asString
        } else {
            JsonParser.parseString(recognizer!!.partialResult).asJsonObject.get("partial")?.asString
        }
    }

    private fun getMicrophone(): Microphone? {
        if (microphone != null) {
            return microphone
        }
        try {
            microphone = SpeechToText.createMicrophone()
        } catch (e: MicrophoneException) {
            ChatPlus.sendMessage(Component.literal("Invalid Microphone, disabling Speech to Text.").withStyle {
                it.withColor(ChatFormatting.GREEN)
            })
            disabled = true
            ChatPlus.LOGGER.error(e)
        }
        return microphone
    }


}