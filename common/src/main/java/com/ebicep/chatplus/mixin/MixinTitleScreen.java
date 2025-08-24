package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.config.Config;
import com.ebicep.chatplus.features.chatwindows.ChatWindow;
import net.minecraft.client.gui.screens.TitleScreen;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    @Inject(method = "init", at = @At(value = "TAIL"))
    private void chatPlus$init(CallbackInfo ci) {
        for (@NotNull ChatWindow chatWindow : Config.INSTANCE.getValues().getChatWindows()) {
            chatWindow.getRenderer().updateCachedDimension();
            chatWindow.getTabSettings().updateTabSettings(null);
        }
    }

}
