package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.features.DeleteMessages;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {

    @Inject(method = "handleDebugKeys", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;clearMessages(Z)V"))
    private void handleDebugKeys(int i, CallbackInfoReturnable<Boolean> cir) {
        DeleteMessages.INSTANCE.f3D();
    }

}
