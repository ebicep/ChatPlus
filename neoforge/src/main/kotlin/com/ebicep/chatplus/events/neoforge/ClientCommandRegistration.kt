package com.ebicep.chatplus.events.neoforge

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.ConfigScreen
import com.ebicep.chatplus.features.Debug
import com.ebicep.chatplus.hud.ChatManager
import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
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
                    ChatManager.selectedTab.clear()
                    Command.SINGLE_SUCCESS
                }
            )
            .then(Commands.literal("hide")
                .executes {
                    Config.values.hideChatEnabled = !Config.values.hideChatEnabled
                    Command.SINGLE_SUCCESS
                }
            )
            .then(
                Commands.literal("debug")
                    .executes {
                        Debug.debug = !Debug.debug
                        ChatPlus.sendMessage(Component.literal("Debug ${if (Debug.debug) "Enabled" else "Disabled"}").withStyle {
                            it.withColor(if (Debug.debug) ChatFormatting.GREEN else ChatFormatting.RED)
                        })
                        Command.SINGLE_SUCCESS
                }
            )
            .executes {
                ConfigScreen.open = true
                Command.SINGLE_SUCCESS
            }

}