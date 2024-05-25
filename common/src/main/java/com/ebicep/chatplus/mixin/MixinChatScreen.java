package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.IChatScreen;
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

    @Unique
    private ChatScreen thisScreen() {
        return (ChatScreen) (Object) this;
    }

    @Inject(method = "init", at = @At("HEAD"))
    public void initHead(CallbackInfo ci) {
        ChatPlusScreenAdapter.INSTANCE.handleInitPre(thisScreen());
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void initTail(CallbackInfo ci) {
        ChatPlusScreenAdapter.INSTANCE.handleInitPost(thisScreen());
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ChatScreen$1;<init>(Lnet/minecraft/client/gui/screens/ChatScreen;Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;)V"), index = 2)
    public int modifyChatScreenStartX(int width) {
        return 2;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ChatScreen$1;<init>(Lnet/minecraft/client/gui/screens/ChatScreen;Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;)V"), index = 3)
    public int modifyChatScreenStartY(int width) {
        return height - ChatPlusScreen.EDIT_BOX_HEIGHT + 4;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ChatScreen$1;<init>(Lnet/minecraft/client/gui/screens/ChatScreen;Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;)V"), index = 4)
    public int modifyChatScreenWidth(int width) {
        return chatPlus$w + 1;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ChatScreen$1;<init>(Lnet/minecraft/client/gui/screens/ChatScreen;Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;)V"), index = 5)
    public int modifyChatScreenHeight(int height) {
        return ChatPlusScreen.EDIT_BOX_HEIGHT;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;setMaxLength(I)V"))
    public int modifyChatScreenMaxLength(int maxLength) {
        return 256 * 5;
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;setCanLoseFocus(Z)V"))
    public boolean modifyChatScreenCanLoseFocus(boolean canLoseFocus) {
        return true;
    }

    @Inject(method = "removed", at = @At("HEAD"))
    public void removed(CallbackInfo ci) {
        ChatPlusScreenAdapter.INSTANCE.handleRemoved(thisScreen());
    }

    @Inject(method = "onEdited", at = @At("HEAD"), cancellable = true)
    public void onEdited(String str, CallbackInfo ci) {
        if (ChatPlusScreenAdapter.INSTANCE.handleOnEdited(thisScreen(), str)) {
            ci.cancel();
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void keyPressed(int key, int scancode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (ChatPlusScreenAdapter.INSTANCE.handleKeyPressed(thisScreen(), key, scancode, modifiers)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;scrollChat(I)V", ordinal = 0))
    public void keyPressedPageUp(int key, int scancode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        ChatPlusScreenAdapter.INSTANCE.handlePageUpDown(true);
    }

    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;scrollChat(I)V", ordinal = 1))
    public void keyPressedPageDown(int key, int scancode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        ChatPlusScreenAdapter.INSTANCE.handlePageUpDown(false);
    }

    @Inject(method = "mouseScrolled", at = @At(value = "RETURN", opcode = 1), cancellable = true)
    public void mouseScrolled(double mouseX, double mouseY, double amountX, double amountY, CallbackInfoReturnable<Boolean> cir) {
        if (ChatPlusScreenAdapter.INSTANCE.handleMouseScrolled(thisScreen(), mouseX, mouseY, amountX, amountY)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @ModifyExpressionValue(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;mouseClicked(DDI)Z"))
    public boolean mouseClickedCommandSuggestions(boolean original, @Share("clicked") LocalBooleanRef booleanRef) {
        booleanRef.set(original);
        return original;
    }

    @Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;mouseClicked(DDI)Z", shift = At.Shift.AFTER), cancellable = true)
    public void mouseClickedAfter(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir, @Share("clicked") LocalBooleanRef booleanRef) {
        if (booleanRef.get()) {
            return;
        }
        if (ChatPlusScreenAdapter.INSTANCE.handleMouseClicked(thisScreen(), mouseX, mouseY, button)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @ModifyExpressionValue(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;mouseClicked(DDI)Z"))
    public boolean mouseClickedEditBox(boolean original) {
        return input.isFocused() && original;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return ChatPlusScreenAdapter.INSTANCE.handleMouseReleased(thisScreen(), mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        ChatPlusScreenAdapter.INSTANCE.handleMouseDragged(thisScreen(), mouseX, mouseY, button, deltaX, deltaY);
        if (!ChatManager.INSTANCE.isChatFocused() || button != 0) { // forgot why this is here
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return true;
    }

    @Inject(method = "moveInHistory", at = @At("HEAD"), cancellable = true)
    public void moveInHistory(int i, CallbackInfo ci) {
        ChatPlusScreenAdapter.INSTANCE.handleMoveInHistory(thisScreen(), i);
        ci.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"))
    public void renderHead(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ChatPlusScreenAdapter.INSTANCE.handleRenderHead(thisScreen(), guiGraphics, mouseX, mouseY, partialTick);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"), index = 0)
    public int renderFillStartY(int y) {
        if (LanguageManager.INSTANCE.getLanguageSpeakEnabled()) {
            return 65;
        } else {
            return 0;
        }
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"), index = 1)
    public int renderFillStartX(int x) {
        return height - ChatPlusScreen.EDIT_BOX_HEIGHT;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"), index = 2)
    public int renderFillWidth(int x) {
        return chatPlus$w;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"), index = 3)
    public int renderFillHeight(int x) {
        return height;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;getMessageTagAt(DD)Lnet/minecraft/client/GuiMessageTag;"))
    public GuiMessageTag renderModifyVariable(GuiMessageTag original) {
        return null;
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void renderTail(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ChatPlusScreenAdapter.INSTANCE.handleRenderTail(thisScreen(), guiGraphics, mouseX, mouseY, partialTick);
    }

    @Inject(method = "getComponentStyleAt", at = @At(value = "HEAD"), cancellable = true)
    public void getComponentStyleAtRedirect(double mouseX, double mouseY, CallbackInfoReturnable<Style> cir) {
        cir.setReturnValue(ChatManager.INSTANCE.getSelectedTab().getComponentStyleAt(mouseX, mouseY));
        cir.cancel();
    }

    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    public void handleChatInput(String string, boolean bl, CallbackInfo ci) {
        ChatPlusScreenAdapter.INSTANCE.handleChatInput(thisScreen(), string);
        ci.cancel();
    }

}
