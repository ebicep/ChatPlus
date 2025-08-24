package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.config.Config;
import com.ebicep.chatplus.features.chatwindows.ChatWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Duration;
import java.util.function.Consumer;

@Mixin(ClientHandshakePacketListenerImpl.class)
public class MixinClientHandshakePacketListenerImpl {

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void chatPlus$init(
            Connection connection,
            Minecraft minecraft,
            ServerData serverData,
            Screen screen,
            boolean bl,
            Duration duration,
            Consumer<Component> consumer,
            CallbackInfo ci
    ) {
        if (serverData != null) {
            String ip = serverData.ip;
            for (@NotNull ChatWindow chatWindow : Config.INSTANCE.getValues().getChatWindows()) {
                chatWindow.getRenderer().updateCachedDimension();
                chatWindow.getTabSettings().updateTabSettings(ip);
            }
        }
    }

}
