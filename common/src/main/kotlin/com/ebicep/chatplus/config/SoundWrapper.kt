package com.ebicep.chatplus.config

import kotlinx.serialization.Serializable
import net.minecraft.sounds.SoundSource

@Serializable
class SoundWrapper {

    var sound: String = ""
    var source: SoundSource = SoundSource.MASTER
    var volume: Float = 1.0f
    var pitch: Float = 1.0f

}