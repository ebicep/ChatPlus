package com.ebicep.chatplus.config.forge

import net.minecraftforge.fml.loading.FMLPaths
import java.nio.file.Path

object ConfigDirectoryImpl {
    /**
     * This is our actual method to [ExampleExpectPlatform.getConfigDirectory].
     */
    @JvmStatic // Jvm Static is required so that java can access it
    fun getConfigDirectory(): Path {
        return FMLPaths.CONFIGDIR.get()
    }
}