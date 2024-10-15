package com.ebicep.chatplus.features.chattabs

import kotlinx.serialization.Serializable

@Serializable
data class TabNotificationSettings(
    var enabled: Boolean = true,
    var scale: Float = .4f,
)
