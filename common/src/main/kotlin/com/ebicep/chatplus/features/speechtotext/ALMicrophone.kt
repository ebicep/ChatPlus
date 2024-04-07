package com.ebicep.chatplus.features.speechtotext

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.features.speechtotext.SpeechToText.canEnumerate
import org.lwjgl.openal.ALC11
import org.lwjgl.openal.ALUtil
import org.lwjgl.openal.EXTFloat32


class ALMicrophone(
    private val sampleRate: Int,
    private val bufferSize: Int,
    private val device: String?,
    private var line: Long = 0L
) : Microphone {

    private var started = false

    init {
        open()
    }

    override fun open() {
        if (isOpen()) {
            throw MicrophoneException("Microphone already open")
        }
        ChatPlus.LOGGER.info("Opening AL Microphone")
        var deviceName = device
        ChatPlus.LOGGER.info("Device: $deviceName")
        val present = canEnumerate()
        if (present) {
            deviceName = ALC11.alcGetString(0L, ALC11.ALC_CAPTURE_DEVICE_SPECIFIER)
            ChatPlus.LOGGER.info("Device: $deviceName")
            checkALCError(0L)
        }
        val device: Long = ALC11.alcCaptureOpenDevice(deviceName, sampleRate, EXTFloat32.AL_FORMAT_MONO_FLOAT32, bufferSize)
        ChatPlus.LOGGER.info("line: $device")
        if (device == 0L) {
            throw MicrophoneException("Failed to open microphone: " + getALCError(ALC11.alcGetError(device)))
        }
        line = device
    }

    override fun close() {
        if (!isOpen()) {
            return
        }
        stopRecording()
        ALC11.alcCaptureCloseDevice(line)
        checkALCError(line)
        line = 0L
    }

    override fun startRecording() {
        if (!isOpen()) {
            return
        }
        if (started) {
            return
        }
        ALC11.alcCaptureStart(line)
        checkALCError(line)
    }

    override fun stopRecording() {
        if (!isOpen()) {
            return
        }
        if (!started) {
            return
        }
        ALC11.alcCaptureStop(line)
        checkALCError(line)
        started = false

        val dataAvailable = dataAvailable()
        val data = FloatArray(dataAvailable)
        ALC11.alcCaptureSamples(line, data, data.size)
        checkALCError(line)
        ChatPlus.LOGGER.info("Cleared $dataAvailable samples")
    }

    override fun isOpen(): Boolean {
        return line != 0L
    }

    override fun isActive(): Boolean {
        return started
    }

    override fun read(): ShortArray {
        val dataAvailable = dataAvailable()
        val buffer = FloatArray(dataAvailable + 1)
        ALC11.alcCaptureSamples(line, buffer, dataAvailable)
        checkALCError(line)
        return ShortArray(buffer.size) {
            (buffer[it] * Short.MAX_VALUE).toInt().toShort()
        }
    }

    override fun dataAvailable(): Int {
        val dataAvailable = ALC11.alcGetInteger(line, ALC11.ALC_CAPTURE_SAMPLES)
        checkALCError(line)
        return dataAvailable
    }

    companion object {
        fun getMicrophoneNames(): List<String> {
            if (!canEnumerate()) {
                return emptyList()
            }
            val devices = ALUtil.getStringList(0L, ALC11.ALC_CAPTURE_DEVICE_SPECIFIER)
            checkALCError(0L)
            return devices ?: emptyList()
        }
    }

}

fun checkALCError(device: Long): Boolean {
    val error = ALC11.alcGetError(device)
    if (error == ALC11.ALC_NO_ERROR) {
        return false
    }
    val stack = Thread.currentThread().stackTrace[2]
    ChatPlus.LOGGER.error("ALC error: {}.{}[{}] {}", stack.className, stack.methodName, stack.lineNumber, getALCError(error))
    return true
}

fun getALCError(i: Int): String {
    return when (i) {
        ALC11.ALC_INVALID_DEVICE -> "Invalid device"
        ALC11.ALC_INVALID_CONTEXT -> "Invalid context"
        ALC11.ALC_INVALID_ENUM -> "Invalid enum"
        ALC11.ALC_INVALID_VALUE -> "Invalid value"
        ALC11.ALC_OUT_OF_MEMORY -> "Out of memory"
        else -> "Unknown error"
    }
}
