package com.ebicep.chatplus

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

const val MOD_ID = "chatplus"

object ChatPlus {

    val LOGGER: Logger = LogManager.getLogger(MOD_ID)

    fun init() {
    }

    fun isEnabled(): Boolean {
        return true
    }
}