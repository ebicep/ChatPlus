package com.ebicep.chatplus.features.chattabs

import kotlinx.serialization.Serializable
import java.awt.Color

@Serializable
data class TabNotificationSettings(
    var enabled: Boolean = true,
    var showCount: Boolean = true,
    var countColor: Int = Color(255, 255, 255, 255).rgb,
    var scale: Float = .4f,
)
