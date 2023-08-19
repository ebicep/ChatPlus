package com.ebicep.chatplus.config.serializers

import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.Serializable

@Serializable
data class KeyWithModifier(
    @Serializable(with = KeySerializer::class)
    var key: InputConstants.Key,
    var modifier: Short
)
