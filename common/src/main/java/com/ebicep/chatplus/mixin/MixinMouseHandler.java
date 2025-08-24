package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.config.Config;
import com.ebicep.chatplus.features.PeekChat;
import com.ebicep.chatplus.hud.ChatPlusScreenAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"), cancellable = true)
    private void onScroll(long l, double d, double e, CallbackInfo ci) {
        if (PeekChat.INSTANCE.getPeeking() && Config.INSTANCE.getValues().getPeekChatScrollingEnabled()) {
            boolean discreteScrolling = Minecraft.getInstance().options.discreteMouseScroll().get();
            double mouseSensitivity = Minecraft.getInstance().options.mouseWheelSensitivity().get();
            double amountY = (discreteScrolling ? Math.signum(e) : e) * mouseSensitivity;
            ChatPlusScreenAdapter.INSTANCE.scrollChat(amountY);
            ci.cancel();
        }
    }

}
