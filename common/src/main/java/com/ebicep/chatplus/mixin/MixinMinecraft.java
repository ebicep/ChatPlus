package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.ChatPlus;
import com.ebicep.chatplus.events.Events;
import com.ebicep.chatplus.hud.ChatPlusScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow
    @Nullable
    public Screen screen;

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Inject(method = "openChatScreen", at = @At("HEAD"))
    public void openChatScreen(String pDefaultText, CallbackInfo ci) {
        if (!ChatPlus.INSTANCE.isEnabled()) {
            return;
        }
        Events.INSTANCE.setLatestDefaultText(pDefaultText);
    }

    @Inject(method = "tick", at = @At(value = "CONSTANT", args = "classValue=net/minecraft/client/gui/screens/InBedChatScreen"))
    public void isBedChatScreen(CallbackInfo ci) {
        if (this.screen instanceof ChatPlusScreen chatPlusScreen && chatPlusScreen.getBedScreen()) {
            if (!this.player.isSleeping()) {
                chatPlusScreen.onPlayerWokeUp();
            }
        }
    }

}
