package com.ebicep.chatplus.config.mutable

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

data class MutableInt(var value: Int = 100)

object MutableIntSerializer : KSerializer<MutableInt> {

    override val descriptor = Int.serializer().descriptor

    override fun deserialize(decoder: Decoder): MutableInt {
        return MutableInt(decoder.decodeInt())
    }

    override fun serialize(encoder: Encoder, value: MutableInt) {
        encoder.encodeInt(value.value)
    }

}