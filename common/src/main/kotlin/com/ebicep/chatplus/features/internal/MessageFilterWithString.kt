package com.ebicep.chatplus.features.internal


import kotlinx.serialization.Serializable

@Serializable
open class MessageFilterWithString : MessageFilter {

    var str: String = ""

    constructor(pattern: String, str: String) : super(pattern) {
        this.str = str
    }

}