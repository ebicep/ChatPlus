package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.ChatPlus;
import com.ebicep.chatplus.config.Config;
import com.ebicep.chatplus.events.EventBus;
import com.ebicep.chatplus.features.chattabs.AddNewMessageEvent;
import com.ebicep.chatplus.features.chattabs.ChatTab;
import com.ebicep.chatplus.features.chattabs.ChatTabs;
import com.ebicep.chatplus.features.chatwindows.ChatWindow;
import com.ebicep.chatplus.features.chatwindows.ChatWindowsManager;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

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
        ChatWindowsManager.INSTANCE.renderAll(guiGraphics, i, j, k);
        ci.cancel();
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("RETURN"))
    public void addMessage(Component component, MessageSignature messageSignature, GuiMessageTag guiMessageTag, CallbackInfo ci) {
        if (!ChatPlus.INSTANCE.isEnabled()) {
            return;
        }
        List<ChatTab> addMessagesTo = new ArrayList<>();
        if (!Config.INSTANCE.getValues().getChatWindowsTabsEnabled()) {
            addMessagesTo.add(ChatTabs.INSTANCE.getDefaultTab());
        } else {
            for (ChatWindow window : Config.INSTANCE.getValues().getChatWindows()) {
                Integer lastPriority = null;
                for (ChatTab chatTab : window.getTabSettings().getSortedTabs()) {
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
        }
    }

}
