package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.ChatPlus;
import com.ebicep.chatplus.events.Events;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "openChatScreen", at = @At("HEAD"))
    public void openChatScreen(String pDefaultText, CallbackInfo ci) {
        if (!ChatPlus.INSTANCE.isEnabled()) {
            return;
        }
        Events.INSTANCE.setLatestDefaultText(pDefaultText);
    }

}
