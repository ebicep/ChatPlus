package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.ChatPlus;
import com.ebicep.chatplus.config.Config;
import com.ebicep.chatplus.features.chattabs.ChatTab;
import com.ebicep.chatplus.features.chattabs.ChatTabs;
import com.ebicep.chatplus.hud.ChatRenderer;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class MixinChatComponent {

    @Final
    @Shadow
    private Minecraft minecraft;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, int k, boolean bl, CallbackInfo ci) {
        if (!ChatPlus.INSTANCE.isEnabled()) {
            return;
        }
        ChatRenderer.INSTANCE.render(guiGraphics, i, j, k);
        ci.cancel();
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("RETURN"))
    public void addMessage(Component component, MessageSignature messageSignature, GuiMessageTag guiMessageTag, CallbackInfo ci) {
        if (!ChatPlus.INSTANCE.isEnabled()) {
            return;
        }
        if (Config.INSTANCE.getValues().getChatTabsEnabled()) {
            Integer lastPriority = null;
            for (ChatTab chatTab : Config.INSTANCE.getValues().getSortedChatTabs()) {
                int priority = chatTab.getPriority();
                boolean alwaysAdd = chatTab.getAlwaysAdd();
                if (lastPriority != null && lastPriority > priority && !alwaysAdd) {
                    continue;
                }
                if (chatTab.matches(component.getString())) {
                    chatTab.addNewMessage(component, messageSignature, this.minecraft.gui.getGuiTicks(), guiMessageTag);
                    if (chatTab.getSkipOthers()) {
                        break;
                    }
                    if (!alwaysAdd) {
                        lastPriority = priority;
                    }
                }
            }
        } else {
            ChatTabs.INSTANCE.getDefaultTab().addNewMessage(component, messageSignature, this.minecraft.gui.getGuiTicks(), guiMessageTag);
        }
    }

}
