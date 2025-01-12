package com.ebicep.chatplus.features.chattabs

import com.ebicep.chatplus.features.internal.MessageFilter
import com.ebicep.chatplus.features.internal.MessageFilterFormatted
import kotlinx.serialization.Serializable

@Serializable
class ServerTabPattern : MessageFilter {

    var chatPattern = MessageFilterFormatted("", false)
    var autoPrefix: String = ""

    constructor(pattern: String, autoPrefix: String) : super(pattern) {
        this.autoPrefix = autoPrefix
    }

}