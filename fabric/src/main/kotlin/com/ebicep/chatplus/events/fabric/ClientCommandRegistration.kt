package com.ebicep.chatplus.events.fabric

import com.ebicep.chatplus.config.ConfigScreen
import com.ebicep.chatplus.hud.ChatManager
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.commands.CommandBuildContext


object ClientCommandRegistration {

    fun registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher: CommandDispatcher<FabricClientCommandSource?>, _: CommandBuildContext? ->
            dispatcher.register(createCommand("chatplus"))
            dispatcher.register(createCommand("cp"))

        })
    }

    private fun createCommand(commandName: String): LiteralArgumentBuilder<FabricClientCommandSource?>? =
        ClientCommandManager.literal(commandName)
            .then(ClientCommandManager.literal("clear")
                .executes {
                    ChatManager.globalSelectedTab.clear()
                    Command.SINGLE_SUCCESS
                }
            )
//            .then(ClientCommandManager.literal("test")
//                .executes {
//                    ChatPlus.doTest()
//                    Command.SINGLE_SUCCESS
//                }
//            )
            .executes {
                ConfigScreen.open = true
                Command.SINGLE_SUCCESS
            }

}