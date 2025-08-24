package com.ebicep.chatplus.features.speechtotext

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.configDirectoryPath
import com.ebicep.chatplus.events.ChatPlusTickEvent
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.internal.MessageFilterWithString
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatPlusScreen
import com.ebicep.chatplus.hud.ChatScreenCloseEvent
import com.ebicep.chatplus.hud.ChatScreenInitPreEvent
import com.ebicep.chatplus.mixin.IMixinChatScreen
import com.ebicep.chatplus.translator.Language
import com.ebicep.chatplus.translator.LanguageManager
import com.ebicep.chatplus.translator.TranslateResult
import com.ebicep.chatplus.translator.Translator
import com.ebicep.chatplus.util.GraphicsUtil.createPose
import com.ebicep.chatplus.util.GraphicsUtil.translate0
import com.ebicep.chatplus.util.KeyUtil.isDown
import com.google.gson.JsonParser
import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientGuiEvent
import dev.architectury.event.events.client.ClientRawInputEvent
import kotlinx.serialization.Serializable
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import org.lwjgl.openal.ALC11
import org.vosk.Model
import org.vosk.Recognizer
import java.awt.Color
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import kotlin.math.min


class MicrophoneException(override val message: String) : Exception(message)

object SpeechToText {

    private val DEVICE_NAME = Pattern.compile("^(?:OpenAL.+?on )?(.*)$")
    val LISTENING_COLOR = Color(0, 200, 0, 255).rgb
    val FAILED_COLOR = Color(200, 0, 0, 255).rgb
    val SAMPLE_RATE: Int
        get() = Config.values.speechToTextSampleRate

    @Volatile
    var recordMic = false
    val microphoneThread = MicrophoneThread()
    private var alFailed = false

    var speechToTextLang: Language? = null

    internal fun renderBoxAndText(guiGraphics: GuiGraphics, text: String, color: Int) {
        val centerWidth = Minecraft.getInstance().window.guiScaledWidth / 2
        val width = Minecraft.getInstance().font.width(text)
        guiGraphics.fill(
            centerWidth - width / 2 - 5,
            35,
            centerWidth + width / 2 + 5,
            52,
            2130706432
        )
        guiGraphics.drawCenteredString(
            Minecraft.getInstance().font,
            text,
            centerWidth,
            40,
            color
        )
        if (Config.values.speechToTextTranslateEnabled) {
            guiGraphics.renderOutline(
                centerWidth - width / 2 - 5,
                35,
                width + 10,
                17,
                (0xFF55FF55).toInt()
            )
        }
    }

    init {
        microphoneThread.start()
        EventBus.register<ChatPlusTickEvent> {
            if (!Config.values.speechToTextEnabled) {
                return@register
            }
            recordMic = Config.values.speechToTextMicrophoneKey.isDown() && !ChatManager.isChatFocused()
        }
    }

    fun getAllPossibleModels(): MutableList<String> {
        val modelDirectory = File("$configDirectoryPath/models")
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

    fun updateTranslateLanguage() {
        speechToTextLang = LanguageManager.findLanguageFromName(Config.values.speechToTextTranslateLang)
    }

}

class MicrophoneThread : Thread("ChatPlusMicrophoneThread") {

    private var listening = false

    @Volatile
    private var running = true

    @Volatile
    private var disabled = false

    @Volatile
    private var microphone: Microphone? = null

    @Volatile
    private var recognizer: Recognizer? = null

    private var lastSpokenMessage: String? = null
    private var totalData: MutableList<Short> = mutableListOf()
    private var quickSendTimer: Long = 0 // -1 = quick sended, 0 = idle
    private val canQuickSend: Boolean
        get() = quickSendTimer > System.currentTimeMillis() && lastSpokenMessage != null

    init {
        ChatPlus.LOGGER.info("SpeechToText initialized")
        EventBus.register<ChatScreenInitPreEvent> {
            if (!Config.values.speechToTextEnabled) {
                return@register
            }
            SpeechToText.recordMic = false
            if (quickSendTimer <= 0) {
                return@register
            }
            doWithMessage { message, translated ->
                if (
                    !Config.values.speechToTextToInputBox && !translated ||
                    !Config.values.speechToTextTranslateToInputBox && translated
                ) {
                    return@doWithMessage
                }
                ChatPlus.LOGGER.info("Quick Send: $message")
                // for if translating messages enabled, takes time so input might already be initialized
                it.screen as IMixinChatScreen
                if (it.screen.input != null) {
                    it.screen.input?.insertText(message)
                } else {
                    it.screen.initial = message
                }
            }
        }
        EventBus.register<ChatScreenCloseEvent> {
            lastSpokenMessage = null
        }
        ClientGuiEvent.RENDER_HUD.register { guiGraphics, tickDelta ->
            if (!Config.values.speechToTextEnabled) {
                return@register
            }
            if (listening) {
                SpeechToText.renderBoxAndText(guiGraphics, "Listening", SpeechToText.LISTENING_COLOR)
            }
            if (canQuickSend) {
                val failed = lastSpokenMessage.isNullOrEmpty()
                val centerWidth = Minecraft.getInstance().window.guiScaledWidth / 2
                val poseStack = guiGraphics.pose()
                poseStack.createPose {
                    if (failed) {
                        SpeechToText.renderBoxAndText(guiGraphics, "Failed", SpeechToText.FAILED_COLOR)
                    } else {
                        SpeechToText.renderBoxAndText(guiGraphics, lastSpokenMessage!!, -1)
                    }
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
                            SpeechToText.LISTENING_COLOR
                        )
                    }
                }
            }
        }
        ClientRawInputEvent.KEY_PRESSED.register { _, keyCode, _, _, _ ->
            if (!Config.values.speechToTextEnabled) {
                return@register EventResult.pass()
            }
            val quickSend = keyCode == Config.values.speechToTextQuickSendKey.value && !ChatManager.isChatFocused()
            if (canQuickSend && quickSend) {
                quickSendTimer = -1
                doWithMessage { message, _ ->
                    ChatPlusScreen.sendChatMessage(message = message)
                }
            }
            EventResult.pass()
        }
    }

    private fun doWithMessage(toRun: (String, Boolean) -> Unit) {
        lastSpokenMessage?.let {
            val speechToTextLang = SpeechToText.speechToTextLang
            if (Config.values.speechToTextTranslateEnabled && speechToTextLang != null) {
                object : Translator(it, LanguageManager.autoLang, speechToTextLang, false) {
                    override fun onTranslate(matchedRegex: String?, translatedMessage: TranslateResult, fromLanguage: String?) {
                        toRun(translatedMessage.translatedText, true)
                    }
                }.start()
            } else {
                toRun(it, false)
            }
        }
    }

    fun resetRecognizer() {
        ChatPlus.LOGGER.info("Resetting Recognizer")
        disabled = false
        recognizer = null
    }

    fun resetMicrophone() {
        ChatPlus.LOGGER.info("Resetting Microphone")
        disabled = false
        microphone = null
    }

    override fun run() {
        ChatPlus.LOGGER.info("SpeechToText Thread started")
        while (running) {
            if (!Config.values.speechToTextEnabled || disabled) {
                sleep(5_000)
                continue
            }
            if (recognizer == null) {
                try {
                    recognizer = Recognizer(
                        Model("$configDirectoryPath/models/" + Config.values.speechToTextSelectedAudioModel),
                        SpeechToText.SAMPLE_RATE.toFloat()
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
                    ChatPlus.LOGGER.info("Started Recording")
                    getMicrophone()?.startRecording()
                    listening = true
                } else if (!recordMic && listening) {
                    ChatPlus.LOGGER.info("Done Recording")
                    listening = false
                    totalData.addAll(readMic().toList())
                    ChatPlus.LOGGER.info("Data: ${totalData.size}")
                    getMicrophone()?.stopRecording()
                    speechToText(totalData.toShortArray())
                    val asString = JsonParser.parseString(recognizer!!.finalResult).asJsonObject.get("text")?.asString
                    lastSpokenMessage = if (asString == null) {
                        null
                    } else {
                        String(asString.toByteArray(charset(Config.values.speechToTextCharset)), StandardCharsets.UTF_8)
                    }
                    quickSendTimer = System.currentTimeMillis() + 3000
                    ChatPlus.LOGGER.info("Final: $lastSpokenMessage")
                    if (lastSpokenMessage.isNullOrBlank()) {
                        continue
                    }
                    Config.values.speechToTextReplace
                        .sortedBy { -it.priority }
                        .filter { it.regex.pattern.isNotEmpty() }
                        .forEach {
                            lastSpokenMessage = lastSpokenMessage!!.replace(it.regex, it.str)
                            ChatPlus.LOGGER.info("Replaced: $lastSpokenMessage")
                        }
                    if (Config.values.speechToTextAutoReplacePlayers) {
                        lastSpokenMessage = replacePlayer(lastSpokenMessage!!)
                    }
                    val screen = Minecraft.getInstance().screen
                    if (ChatManager.isChatFocused()) {
                        doWithMessage { message, translated ->
                            if (
                                Config.values.speechToTextToInputBox && !translated ||
                                Config.values.speechToTextTranslateToInputBox && translated
                            ) {
                                return@doWithMessage
                            }
                            screen as IMixinChatScreen
                            screen.input?.insertText(message)
                        }
                    }
                } else if (listening) {
                    ChatPlus.LOGGER.debug("available: ${microphone!!.dataAvailable()}")
                    totalData.addAll(readMic().toList())
                    sleep()
                }
            } catch (e: Exception) {
                ChatPlus.LOGGER.error(e)
                ChatPlus.sendMessage(Component.literal("Problem recording speech, disabling Speech to Text: ${e.message}").withStyle {
                    it.withColor(ChatFormatting.RED)
                })
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
                it.withColor(ChatFormatting.RED)
            })
            disabled = true
            ChatPlus.LOGGER.error(e)
        }
        return microphone
    }

    private fun replacePlayer(input: String, searchDepth: Int = Config.values.speechToTextAutoReplacePlayersMaxSearchDepth): String {
        if (!input.contains("player")) {
            return input
        }
        val players = Minecraft.getInstance().connection?.listedOnlinePlayers ?: return input
        val words = input.substringAfter("player ").split(" ")
        var matched = players
            .map { it.profile.name }
            .map { MatchedPlayer(it, it) }
            .toList()
        var matchedIndex = -1
        for (i in 0 until min(searchDepth, words.size)) {
            val wordToMatch = words[i]
            val newMatched = matched
                .filter {
                    val matchedIndex = it.postMatchName.indexOf(wordToMatch, ignoreCase = true)
                    if (matchedIndex != -1) {
                        it.postMatchName = it.postMatchName.substring(matchedIndex + wordToMatch.length)
                    }
                    matchedIndex != -1
                }
                .toList()
            if (newMatched.isEmpty()) {
                break
            }
            matched = newMatched
            matchedIndex = i
            if (newMatched.size == 1) {
                break
            }
        }
        if (matchedIndex == -1 || matched.isEmpty()) {
            return input
        }
        return input.replace(
            "player " + words.subList(0, matchedIndex + 1).joinToString(" "),
            matched.first().name,
            ignoreCase = true
        )
    }

    data class MatchedPlayer(val name: String, var postMatchName: String)

    @Serializable
    class SpeechToTextReplace : MessageFilterWithString {

        var priority: Int = 0

        constructor(pattern: String, str: String, priority: Int) : super(pattern, str) {
            this.priority = priority
        }

    }

}