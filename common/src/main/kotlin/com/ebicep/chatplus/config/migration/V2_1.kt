@file:UseSerializers(
    KeySerializer::class
)

package com.ebicep.chatplus.config.migration

import com.ebicep.chatplus.MOD_ID
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.serializers.KeySerializer
import com.ebicep.chatplus.config.serializers.KeyWithModifier
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers


object V2_1 : Migrator<SchemaV2_1> {

    override fun getFileNameVersion(): String {
        return "$MOD_ID-v2.1.0"
    }

    override fun getSerializer(): KSerializer<SchemaV2_1> {
        return SchemaV2_1.serializer()
    }

    override fun migrate(old: SchemaV2_1) {
        val values = Config.values
        values.movableChatKey = KeyWithModifier(old.movableChatToggleKey, 0)
    }

}

@Serializable
data class SchemaV2_1(

    var movableChatToggleKey: InputConstants.Key = InputConstants.getKey("key.keyboard.right.control"),


    )

