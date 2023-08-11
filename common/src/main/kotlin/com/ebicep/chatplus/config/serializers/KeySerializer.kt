package com.ebicep.chatplus.config.serializers

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.InputConstants.Key
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object KeySerializer : KSerializer<Key> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.mojang.blaze3d.platform.InputConstants.Key", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Key {
        return InputConstants.getKey(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Key) {
        encoder.encodeString(value.name)
    }

}