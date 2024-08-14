package com.ebicep.chatplus.events.neoforge

import com.ebicep.chatplus.config.ConfigScreen
import com.ebicep.chatplus.hud.ChatManager
import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent


@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
object ClientCommandRegistration {

    @SubscribeEvent
    fun registerCommands(event: RegisterClientCommandsEvent) {
        event.dispatcher.register(createCommand("chatplus"))
        event.dispatcher.register(createCommand("cp"))
    }

    private fun createCommand(commandName: String): LiteralArgumentBuilder<CommandSourceStack>? =
        Commands.literal(commandName)
            .then(Commands.literal("clear")
                .executes {
                    ChatManager.globalSelectedTab.clear()
                    Command.SINGLE_SUCCESS
                }
            )
            .executes {
                ConfigScreen.open = true
                Command.SINGLE_SUCCESS
            }

}