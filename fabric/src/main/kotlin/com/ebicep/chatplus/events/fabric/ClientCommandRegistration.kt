package com.ebicep.chatplus.events.fabric

import com.ebicep.chatplus.config.ConfigScreen
import com.ebicep.chatplus.hud.ChatManager
import com.ebicep.chatplus.hud.ChatRenderer
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.commands.CommandBuildContext

object ClientCommandRegistration {

    fun registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher: CommandDispatcher<FabricClientCommandSource?>, _: CommandBuildContext? ->
            val command = dispatcher.register(
                ClientCommandManager.literal("chatplus")
                    .executes {
                        ConfigScreen.open = true
                        Command.SINGLE_SUCCESS
                    }
                    .then(ClientCommandManager.literal("clear")
                        .executes {
                            ChatManager.selectedTab.clear()
                            Command.SINGLE_SUCCESS
                        }
                    )
                    .then(ClientCommandManager.literal("test")
                        .executes {
                            ChatRenderer.updateCachedDimension()
                            Command.SINGLE_SUCCESS
                        }
                    )
            )
            dispatcher.register(ClientCommandManager.literal("cp").redirect(command))
        })
    }

}