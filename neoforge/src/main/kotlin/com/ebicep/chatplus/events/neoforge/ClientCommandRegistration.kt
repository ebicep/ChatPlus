package com.ebicep.chatplus.events.neoforge

import com.ebicep.chatplus.config.ConfigScreen
import com.mojang.brigadier.Command
import net.minecraft.commands.Commands
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent


@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
object ClientCommandRegistration {

    @SubscribeEvent
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