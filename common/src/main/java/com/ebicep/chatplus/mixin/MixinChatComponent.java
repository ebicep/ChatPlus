package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.ChatPlus;
import com.ebicep.chatplus.hud.ChatManager;
import com.ebicep.chatplus.hud.ChatTab;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class MixinChatComponent {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, int k, CallbackInfo ci) {
        if (!ChatPlus.INSTANCE.isEnabled()) {
            return;
        }
        ChatManager.INSTANCE.getSelectedTab().render(guiGraphics);
        ci.cancel();
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V", at = @At("RETURN"))
    public void setChatLine(Component pChatComponent, MessageSignature pHeaderSignature, int pAddedTime, GuiMessageTag pTag, boolean pOnlyTrim, CallbackInfo ci) {
        if (!ChatPlus.INSTANCE.isEnabled()) {
            return;
        }
        for (ChatTab chatTab : ChatManager.INSTANCE.getChatTabs()) {
            chatTab.addMessage(pChatComponent, pHeaderSignature, pAddedTime, pTag, pOnlyTrim);
        }
    }

}
