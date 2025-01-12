package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.features.chattabs.ChatTab;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerList.class)
public class MixinServerList {

    @Inject(
            method = "load",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
                    ordinal = 1
            )
    )
    private void onLoad(CallbackInfo ci, @Local ServerData serverData) {
        ChatTab.Companion.getCachedServerIPs().add(serverData.ip);
    }

}
