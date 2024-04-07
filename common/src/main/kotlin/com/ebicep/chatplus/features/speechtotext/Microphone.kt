package com.ebicep.chatplus.features.speechtotext

interface Microphone {

    fun open()

    fun close()

    fun startRecording()

    fun stopRecording()

    fun isOpen(): Boolean

    fun isActive(): Boolean

    fun read(): ShortArray

    fun dataAvailable(): Int

}