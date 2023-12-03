package com.ebicep.chatplus

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.Events
import com.ebicep.chatplus.translator.LanguageManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

const val MOD_ID = "chatplus"

object ChatPlus {

    val LOGGER: Logger = LogManager.getLogger(MOD_ID)

    fun init() {
        LanguageManager
        Config.load()
        LanguageManager.updateTranslateLanguages()

        Events
    }

    fun isEnabled(): Boolean {
        return Config.values.enabled
    }
}