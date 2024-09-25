package com.ebicep.chatplus.config.migration

import com.ebicep.chatplus.MOD_ID
import com.ebicep.chatplus.config.Config
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable


object V2 : Migrator<SchemaV2> {

    override fun getFileNameVersion(): String {
        return "$MOD_ID-v2"
    }

    override fun getSerializer(): KSerializer<SchemaV2> {
        return SchemaV2.serializer()
    }

    override fun migrate(old: SchemaV2) {
        val values = Config.values
        values.compactMessageSettings.ignoreTimestamps = old.compactMessagesIgnoreTimestamps
    }

}

@Serializable
data class SchemaV2(

    var compactMessagesIgnoreTimestamps: Boolean = false

)

