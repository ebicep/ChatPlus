package com.ebicep.chatplus.events.forge

import com.ebicep.chatplus.config.ConfigScreen
import com.mojang.brigadier.Command
import net.minecraft.commands.Commands
import net.minecraftforge.client.event.RegisterClientCommandsEvent


object ClientCommandRegistration {

    fun registerCommands(event: RegisterClientCommandsEvent) {
        val commandNode = event.dispatcher.register(
            Commands.literal("chatplus")
                .executes {
                    ConfigScreen.open = true
                    Command.SINGLE_SUCCESS
                }
        )
        event.dispatcher.register(Commands.literal("cp").redirect(commandNode))
    }

}