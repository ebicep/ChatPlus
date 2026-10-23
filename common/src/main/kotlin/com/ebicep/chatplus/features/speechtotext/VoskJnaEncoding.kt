package com.ebicep.chatplus.features.speechtotext

private const val JNA_ENCODING_PROPERTY = "jna.encoding"
private const val VOSK_JNA_ENCODING = "ISO-8859-1"

object VoskJnaEncoding {

    init {
        if (System.getProperty(JNA_ENCODING_PROPERTY) == null) {
            System.setProperty(JNA_ENCODING_PROPERTY, VOSK_JNA_ENCODING)
        }
    }

    @JvmStatic
    fun ensure() {
        // Triggers init block before VOSK/JNA marshals native strings.
    }

    internal fun decodeVoskUtf8(text: String): String =
        try {
            String(text.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
        } catch (_: Exception) {
            text
        }
}
