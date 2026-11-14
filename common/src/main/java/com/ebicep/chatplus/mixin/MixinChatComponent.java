package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.ChatPlus;
import com.ebicep.chatplus.config.Config;
import com.ebicep.chatplus.events.EventBus;
import com.ebicep.chatplus.features.ChatLinks;
import com.ebicep.chatplus.features.chattabs.AddNewMessageEvent;
import com.ebicep.chatplus.features.chattabs.ChatTab;
import com.ebicep.chatplus.features.chattabs.SkipNewMessageEvent;
import com.ebicep.chatplus.features.chatwindows.ChatWindowsManager;
import com.ebicep.chatplus.hud.ChatManager;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = ChatComponent.class, priority = Integer.MAX_VALUE / 2)
public class MixinChatComponent {

    @Final
    @Shadow
    private Minecraft minecraft;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, int k, boolean bl, CallbackInfo ci) {
        if (!ChatPlus.INSTANCE.isEnabled() || (Config.INSTANCE.getValues().getShowVanillaWhenUnfocused() && !ChatManager.INSTANCE.isChatFocused())) {
            return;
        }
        ChatWindowsManager.INSTANCE.renderAll(guiGraphics, i, j, k);
        ci.cancel();
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessageToDisplayQueue(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V")
    )
    private void linkifyBeforeDisplay(
            Component component,
            MessageSignature messageSignature,
            GuiMessageTag guiMessageTag,
            CallbackInfo ci
    ) {
        if (!(component instanceof MutableComponent mutableComponent)) {
            return;
        }
        if (!ChatPlus.INSTANCE.isEnabled() || !Config.INSTANCE.getValues().getChatLinksEnabled()) {
            return;
        }
        ChatLinks.linkify(mutableComponent);
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("RETURN"))
    public void addMessage(
            Component component, MessageSignature messageSignature, GuiMessageTag guiMessageTag,
            CallbackInfo ci,
            @Local GuiMessage guiMessage
    ) {
        try {
            chatPlus$handleAddMessage(guiMessage);
        } catch (Exception e) {
            ChatPlus.INSTANCE.getLOGGER().error("Failed to handle incoming chat message", e);
        }
    }

    private void chatPlus$handleAddMessage(GuiMessage guiMessage) {
        if (!ChatPlus.INSTANCE.isEnabled() && !Config.INSTANCE.getValues().getAddMessagesIfDisabled()) {
            return;
        }
        Component component = guiMessage.content();
        MessageSignature messageSignature = guiMessage.signature();
        GuiMessageTag guiMessageTag = guiMessage.tag();
        List<ChatTab> addMessagesTo = new ArrayList<>();

        Integer lastPriority = null;
        for (ChatTab chatTab : ChatManager.INSTANCE.getGlobalSortedTabs()) {
            int priority = chatTab.getPriority();
            boolean alwaysAdd = chatTab.getAlwaysAdd();
            if (lastPriority != null && lastPriority > priority && !alwaysAdd) {
                continue;
            }
            if (chatTab.matches(component)) {
                addMessagesTo.add(chatTab);
                if (chatTab.getSkipOthers()) {
                    break;
                }
                if (!alwaysAdd) {
                    lastPriority = priority;
                }
            }
        }
        if (!addMessagesTo.isEmpty()) {
            AddNewMessageEvent messageEvent = new AddNewMessageEvent(
                    component.copy(),
                    component,
                    null,
                    messageSignature,
                    this.minecraft.gui.getGuiTicks(),
                    guiMessageTag,
                    false
            );
            EventBus.INSTANCE.post(AddNewMessageEvent.class, messageEvent);
            if (messageEvent.getReturnFunction()) {
                return;
            }
            for (ChatTab chatTab : addMessagesTo) {
                chatTab.addNewMessage(messageEvent);
            }
        } else {
            SkipNewMessageEvent messageEvent = new SkipNewMessageEvent(
                    component.copy(),
                    component,
                    null,
                    messageSignature,
                    this.minecraft.gui.getGuiTicks(),
                    guiMessageTag
            );
            EventBus.INSTANCE.post(SkipNewMessageEvent.class, messageEvent);
        }
    }

}