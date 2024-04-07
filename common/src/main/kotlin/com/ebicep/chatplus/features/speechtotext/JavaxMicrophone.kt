package com.ebicep.chatplus.features.speechtotext

import com.ebicep.chatplus.ChatPlus
import javax.sound.sampled.*
import javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED

class JavaxMicrophone(
    private val sampleRate: Int,
    private val device: String?,
    private var dataLine: TargetDataLine? = null
) : Microphone {

    init {
        open()
    }

    override fun open() {
        if (isOpen()) {
            throw MicrophoneException("Microphone already open")
        }
        ChatPlus.LOGGER.info("Opening Javax Microphone")

        val audioFormat = AudioFormat(PCM_SIGNED, sampleRate.toFloat(), 16, 1, 2, sampleRate.toFloat(), false)

        AudioSystem.getMixerInfo().forEach {
            val mixer = AudioSystem.getMixer(it)
            val info = DataLine.Info(TargetDataLine::class.java, audioFormat)
            if (mixer.isLineSupported(info) && it.name == device) {
                dataLine = mixer.getLine(info) as TargetDataLine
                ChatPlus.LOGGER.info("Found microphone: ${it.name}")
                return@forEach
            }
        }
        if (dataLine == null) {
            dataLine = AudioSystem.getLine(DataLine.Info(TargetDataLine::class.java, audioFormat)) as TargetDataLine
            ChatPlus.LOGGER.info("Using default microphone")
        }
        if (dataLine == null) {
            throw MicrophoneException("No microphone found")
        }
        try {
            dataLine!!.open(audioFormat)
            ChatPlus.LOGGER.info("Microphone opened successfully")
        } catch (e: LineUnavailableException) {
            throw MicrophoneException(e.message.toString())
        }

//        dataLine!!.start()
//        dataLine!!.stop()
//        dataLine!!.flush()

    }

    override fun close() {
        if (!isOpen()) {
            return
        }
        dataLine!!.stop()
        dataLine!!.flush()
        dataLine!!.close()
    }

    override fun startRecording() {
        if (!isOpen()) {
            return
        }
        dataLine!!.start()
    }

    override fun stopRecording() {
        if (!isOpen()) {
            return
        }
        dataLine!!.stop()
    }

    override fun isActive(): Boolean {
        if (dataLine == null) {
            return false
        }
        return dataLine!!.isActive
    }

    override fun read(): ShortArray {
        if (!isOpen()) {
            throw MicrophoneException("Microphone is not open")
        }
        val dataAvailable = dataAvailable()
        val buffer = ByteArray(dataAvailable + 1)
        dataLine!!.read(buffer, 0, buffer.size)
        return ShortArray(buffer.size / 2) {
            (buffer[it * 2 + 1].toInt() shl 8 or (buffer[it * 2].toInt() and 0xFF)).toShort()
        }
    }

    override fun dataAvailable(): Int {
        if (dataLine == null) {
            return 0
        }
        return dataLine!!.available()
    }

    override fun isOpen(): Boolean {
        if (dataLine == null) {
            return false
        }
        return dataLine!!.isOpen
    }

    companion object {
        fun getMicrophoneNames(): List<String> {
            val audioFormat = AudioFormat(PCM_SIGNED, SAMPLE_RATE.toFloat(), 16, 1, 2, SAMPLE_RATE.toFloat(), false)
            val names = mutableListOf<String>()
            AudioSystem.getMixerInfo().forEach {
                val mixer = AudioSystem.getMixer(it)
                val info = DataLine.Info(TargetDataLine::class.java, audioFormat);
                if (mixer.isLineSupported(info)) {
                    names.add(it.name)
                }
            }
            return names
        }
    }

}