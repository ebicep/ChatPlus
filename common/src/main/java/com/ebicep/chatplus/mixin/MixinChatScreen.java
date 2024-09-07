package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.IChatScreen;
import com.ebicep.chatplus.config.Config;
import com.ebicep.chatplus.hud.ChatManager;
import com.ebicep.chatplus.hud.ChatPlusScreen;
import com.ebicep.chatplus.hud.ChatPlusScreenAdapter;
import com.ebicep.chatplus.translator.LanguageManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
@Implements(@Interface(iface = IChatScreen.class, prefix = "chatPlus$"))
public abstract class MixinChatScreen extends Screen implements IMixinChatScreen, IChatScreen {

    @Shadow
    protected EditBox input;

    @Unique
    private int chatPlus$w;

    protected MixinChatScreen(Component component) {
        super(component);
    }

    public int chatPlus$getChatPlusWidth() {
        return chatPlus$w;
    }

    public void chatPlus$setChatPlusWidth(int chatPlus$w) {
        this.chatPlus$w = chatPlus$w;
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void initHead(CallbackInfo ci) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        ChatPlusScreenAdapter.INSTANCE.handleInitPre(thisScreen());
    }

    @Unique
    private ChatScreen thisScreen() {
        return (ChatScreen) (Object) this;
    }

    @ModifyExpressionValue(method = "init", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"))
    private int initModifyVariable(int original) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return original;
        }
        return ChatManager.INSTANCE.getSentMessages().size();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void initTail(CallbackInfo ci) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        ChatPlusScreenAdapter.INSTANCE.handleInitPost(thisScreen());
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ChatScreen$1;<init>(Lnet/minecraft/client/gui/screens/ChatScreen;Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;)V"), index = 2)
    private int modifyChatScreenStartX(int x) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return x;
        }
        if (Config.INSTANCE.getValues().getVanillaInputBox()) {
            return x;
        }
        return 2;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ChatScreen$1;<init>(Lnet/minecraft/client/gui/screens/ChatScreen;Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;)V"), index = 3)
    private int modifyChatScreenStartY(int y) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return y;
        }
        if (Config.INSTANCE.getValues().getVanillaInputBox()) {
            return y;
        }
        return this.height - ChatPlusScreen.EDIT_BOX_HEIGHT + 4;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ChatScreen$1;<init>(Lnet/minecraft/client/gui/screens/ChatScreen;Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;)V"), index = 4)
    private int modifyChatScreenWidth(int width) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return width;
        }
        return chatPlus$w + 1;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ChatScreen$1;<init>(Lnet/minecraft/client/gui/screens/ChatScreen;Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;)V"), index = 5)
    private int modifyChatScreenHeight(int height) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return height;
        }
        if (Config.INSTANCE.getValues().getVanillaInputBox()) {
            return height;
        }
        return ChatPlusScreen.EDIT_BOX_HEIGHT;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;setMaxLength(I)V"))
    private int modifyChatScreenMaxLength(int maxLength) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return maxLength;
        }
        return 256 * 5;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;setCanLoseFocus(Z)V"))
    private boolean modifyChatScreenCanLoseFocus(boolean canLoseFocus) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return canLoseFocus;
        }
        return true;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;<init>(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/components/EditBox;Lnet/minecraft/client/gui/Font;ZZIIZI)V"), index = 7)
    private int modifyChatScreenCommandSuggestionsMaxHeight(int maxSuggestions) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return maxSuggestions;
        }
        return Config.INSTANCE.getValues().getMaxCommandSuggestions();
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void removed(CallbackInfo ci) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        ChatPlusScreenAdapter.INSTANCE.handleRemoved(thisScreen());
    }

    @Inject(method = "onEdited", at = @At("HEAD"), cancellable = true)
    private void onEdited(String str, CallbackInfo ci) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        if (ChatPlusScreenAdapter.INSTANCE.handleOnEdited(thisScreen(), str)) {
            ci.cancel();
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void keyPressed(int key, int scancode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        if (ChatPlusScreenAdapter.INSTANCE.handleKeyPressed(thisScreen(), key, scancode, modifiers)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;scrollChat(I)V", ordinal = 0))
    private void keyPressedPageUp(int key, int scancode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        ChatPlusScreenAdapter.INSTANCE.handlePageUpDown(true);
    }

    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;scrollChat(I)V", ordinal = 1))
    private void keyPressedPageDown(int key, int scancode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        ChatPlusScreenAdapter.INSTANCE.handlePageUpDown(false);
    }

    @Inject(method = "mouseScrolled", at = @At(value = "RETURN", opcode = 1), cancellable = true)
    private void mouseScrolled(double mouseX, double mouseY, double amountX, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        if (ChatPlusScreenAdapter.INSTANCE.handleMouseScrolled(thisScreen(), mouseX, mouseY, amountX, 0)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @ModifyExpressionValue(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;mouseClicked(DDI)Z"))
    private boolean mouseClickedCommandSuggestions(boolean original, @Share("clicked") LocalBooleanRef booleanRef) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return original;
        }
        booleanRef.set(original);
        return original;
    }

    @Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;mouseClicked(DDI)Z", shift = At.Shift.AFTER), cancellable = true)
    private void mouseClickedAfter(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir, @Share("clicked") LocalBooleanRef booleanRef) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        if (booleanRef.get()) {
            return;
        }
        if (ChatPlusScreenAdapter.INSTANCE.handleMouseClicked(thisScreen(), mouseX, mouseY, button)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @ModifyExpressionValue(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;mouseClicked(DDI)Z"))
    private boolean mouseClickedEditBox(boolean original) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return original;
        }
        return input.isFocused() && original;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        return ChatPlusScreenAdapter.INSTANCE.handleMouseReleased(thisScreen(), mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        ChatPlusScreenAdapter.INSTANCE.handleMouseDragged(thisScreen(), mouseX, mouseY, button, deltaX, deltaY);
        if (!ChatManager.INSTANCE.isChatFocused() || button != 0) { // forgot why this is here
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return true;
    }

    @Override
    public boolean keyReleased(int i, int j, int k) {
        if (Config.INSTANCE.getValues().getEnabled()) {
            ChatPlusScreenAdapter.INSTANCE.handleKeyReleased(thisScreen(), i, j, k);
        }
        return super.keyReleased(i, j, k);
    }

    @Inject(method = "moveInHistory", at = @At("HEAD"), cancellable = true)
    private void moveInHistory(int i, CallbackInfo ci) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        ChatPlusScreenAdapter.INSTANCE.handleMoveInHistory(thisScreen(), i);
        ci.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void renderHead(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        ChatPlusScreenAdapter.INSTANCE.handleRenderHead(thisScreen(), guiGraphics, mouseX, mouseY, partialTick);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"), index = 0)
    private int renderFillStartX(int x) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return x;
        }
        if (LanguageManager.INSTANCE.getLanguageSpeakEnabled()) {
            if (Config.INSTANCE.getValues().getVanillaInputBox()) {
                return 65 + x;
            }
            return 65;
        } else {
            if (Config.INSTANCE.getValues().getVanillaInputBox()) {
                return x;
            }
            return 0;
        }
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"), index = 1)
    private int renderFillStartY(int y) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return y;
        }
        if (Config.INSTANCE.getValues().getVanillaInputBox()) {
            return y;
        }
        return height - ChatPlusScreen.EDIT_BOX_HEIGHT;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"), index = 2)
    private int renderFillWidth(int x) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return x;
        }
        return chatPlus$w;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"), index = 3)
    private int renderFillHeight(int y) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return y;
        }
        if (Config.INSTANCE.getValues().getVanillaInputBox()) {
            return y;
        }
        return height;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;getMessageTagAt(DD)Lnet/minecraft/client/GuiMessageTag;"))
    private GuiMessageTag renderModifyVariable(GuiMessageTag original) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return original;
        }
        return null;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderTail(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        ChatPlusScreenAdapter.INSTANCE.handleRenderTail(thisScreen(), guiGraphics, mouseX, mouseY, partialTick);
    }

    @Inject(method = "getComponentStyleAt", at = @At(value = "HEAD"), cancellable = true)
    private void getComponentStyleAtRedirect(double mouseX, double mouseY, CallbackInfoReturnable<Style> cir) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        cir.setReturnValue(ChatManager.INSTANCE.getGlobalSelectedTab().getComponentStyleAt(mouseX, mouseY));
        cir.cancel();
    }

    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void handleChatInput(String string, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        cir.setReturnValue(ChatPlusScreenAdapter.INSTANCE.handleChatInput(thisScreen(), string));
        cir.cancel();
    }

}
