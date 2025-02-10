package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.ChatPlus;
import com.ebicep.chatplus.config.Config;
import com.ebicep.chatplus.events.EventBus;
import com.ebicep.chatplus.features.chattabs.AddNewMessageEvent;
import com.ebicep.chatplus.features.chattabs.ChatTab;
import com.ebicep.chatplus.features.chattabs.SkipNewMessageEvent;
import com.ebicep.chatplus.features.chatwindows.ChatWindowsManager;
import com.ebicep.chatplus.hud.ChatManager;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = ChatComponent.class, priority = Integer.MAX_VALUE)
public class MixinChatComponent {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, int k, CallbackInfo ci) {
        if (!ChatPlus.INSTANCE.isEnabled() || (Config.INSTANCE.getValues().getShowVanillaWhenUnfocused() && !ChatManager.INSTANCE.isChatFocused())) {
            return;
        }
        ChatWindowsManager.INSTANCE.renderAll(guiGraphics, i, j, k);
        ci.cancel();
    }

    @ModifyArg(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ComponentRenderUtils;wrapComponents(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;"),
            index = 0
    )
    private FormattedText components(FormattedText arg3, @Share("chatplus$component") LocalRef<Component> component) {
        if (arg3 instanceof Component c) {
            component.set(c);
        }
        return arg3;
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V", at = @At("RETURN"))
    public void addMessage(
            Component component,
            MessageSignature messageSignature,
            int i,
            GuiMessageTag guiMessageTag,
            boolean bl,
            CallbackInfo ci,
            @Share("chatplus$component") LocalRef<Component> c
    ) {
        if (!ChatPlus.INSTANCE.isEnabled() && !Config.INSTANCE.getValues().getAddMessagesIfDisabled()) {
            return;
        }
        component = c.get();
        List<ChatTab> addMessagesTo = new ArrayList<>();

        Integer lastPriority = null;
        for (ChatTab chatTab : ChatManager.INSTANCE.getGlobalSortedTabs()) {
            int priority = chatTab.getPriority();
            boolean alwaysAdd = chatTab.getAlwaysAdd();
            if (lastPriority != null && lastPriority > priority && !alwaysAdd) {
                continue;
            }
            if (chatTab.matches(component.getString())) {
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
                    i,
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
                    i,
                    guiMessageTag
            );
            EventBus.INSTANCE.post(SkipNewMessageEvent.class, messageEvent);
        }
    }

}
