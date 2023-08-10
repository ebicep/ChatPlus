package com.ebicep.chatplus.config.mutable

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

data class MutableBoolean(var value: Boolean = true)

object MutableBooleanSerializer : KSerializer<MutableBoolean> {

    override val descriptor = Boolean.serializer().descriptor

    override fun deserialize(decoder: Decoder): MutableBoolean {
        return MutableBoolean(decoder.decodeBoolean())
    }

    override fun serialize(encoder: Encoder, value: MutableBoolean) {
        encoder.encodeBoolean(value.value)
    }

}