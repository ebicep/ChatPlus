package com.ebicep.chatplus.config.migration

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config.values
import com.ebicep.chatplus.config.ConfigVariables
import com.ebicep.chatplus.config.json
import kotlinx.serialization.KSerializer
import java.io.File

object MigrationManager {

    private val migrators: List<Migrator<*>> = listOf(
        V1
    )

    fun tryMigration(configDirectory: File, currentConfig: File): Boolean {
        ChatPlus.LOGGER.info("Checking for config migration")
        migrators.forEach {
            val oldConfigFileName = "${it.getFileNameVersion()}.json"
            val oldConfig = File(configDirectory, oldConfigFileName)
            if (oldConfig.exists()) {
                ChatPlus.LOGGER.info("Migrating config from $oldConfigFileName")
                // update current values with old values
                values = json.decodeFromString(ConfigVariables.serializer(), oldConfig.readText())
                // update the config with migrated values
                val oldConfigValues: Any? = json.decodeFromString(it.getSerializer(), oldConfig.readText())
                (it as Migrator<Any?>).migrate(oldConfigValues)
                // write new values
                currentConfig.writeText(json.encodeToString(ConfigVariables.serializer(), values))
                return true
            }
        }
        return false
    }

    fun copyFile(file: File, newFile: File) {
        file.copyTo(newFile)
    }

}

interface Migrator<T> {

    fun getFileNameVersion(): String

    fun getSerializer(): KSerializer<T>

    fun migrate(old: T)

}