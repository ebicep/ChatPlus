package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.ChatPlus;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class MixinChatScreen {

    @Inject(method = "removed", at = @At("HEAD"), cancellable = true)
    public void removed(CallbackInfo ci) {
        if (ChatPlus.INSTANCE.isEnabled() || ((ChatScreen) (Object) this).minecraft == null) {
            ci.cancel();
        }
    }

}
