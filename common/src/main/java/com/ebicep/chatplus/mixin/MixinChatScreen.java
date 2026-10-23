package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.IChatScreen;
import com.ebicep.chatplus.config.Config;
import com.ebicep.chatplus.features.MovableChat;
import com.ebicep.chatplus.features.TranslateMessage;
import com.ebicep.chatplus.hud.ChatManager;
import com.ebicep.chatplus.hud.ChatPlusScreen;
import com.ebicep.chatplus.hud.ChatPlusScreenAdapter;
import com.ebicep.chatplus.translator.LanguageManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChatScreen.class, priority = Integer.MAX_VALUE / 2)
@Implements(@Interface(iface = IChatScreen.class, prefix = "chatPlus$"))
public abstract class MixinChatScreen extends Screen implements IMixinChatScreen, IChatScreen {

    @Shadow
    protected EditBox input;
    @Unique
    private int chatPlus$w;

    protected MixinChatScreen(Component component) {
        super(component);
    }

    @Shadow
    public abstract boolean mouseClicked(@NotNull MouseButtonEvent mouseButtonEvent, boolean bl);

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent mouseButtonEvent) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return super.mouseReleased(mouseButtonEvent);
        }
        return ChatPlusScreenAdapter.INSTANCE.handleMouseReleased(thisScreen(), mouseButtonEvent);
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return super.mouseDragged(mouseButtonEvent, deltaX, deltaY);
        }
        ChatPlusScreenAdapter.INSTANCE.handleMouseDragged(thisScreen(), mouseButtonEvent, deltaX, deltaY);
        if (!ChatManager.INSTANCE.isChatFocused() || mouseButtonEvent.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseDragged(mouseButtonEvent, deltaX, deltaY);
        }
        return true;
    }

    @Override
    public boolean keyReleased(@NotNull KeyEvent keyEvent) {
        if (Config.INSTANCE.getValues().getEnabled()) {
            ChatPlusScreenAdapter.INSTANCE.handleKeyReleased(thisScreen(), keyEvent);
        }
        return super.keyReleased(keyEvent);
    }

    @Unique
    private ChatScreen thisScreen() {
        return (ChatScreen) (Object) this;
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

    @ModifyExpressionValue(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ArrayListDeque;size()I"))
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
        return Config.INSTANCE.getValues().getInputBoxSettings().getStartX();
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ChatScreen$1;<init>(Lnet/minecraft/client/gui/screens/ChatScreen;Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;)V"), index = 3)
    private int modifyChatScreenStartY(int y) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return y;
        }
        if (Config.INSTANCE.getValues().getVanillaInputBox()) {
            return y;
        }
        return Config.INSTANCE.getValues().getInputBoxSettings().getCalculatedStartY();
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ChatScreen$1;<init>(Lnet/minecraft/client/gui/screens/ChatScreen;Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/network/chat/Component;)V"), index = 4)
    private int modifyChatScreenWidth(int width) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return width;
        }
        return chatPlus$w - 5;
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
        return Config.INSTANCE.getValues().getInputBoxSettings().getMaxInputBoxInputLength();
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

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;<init>(Lnet/minecraft/client/Minecraft;" +
            "Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/components/EditBox;Lnet/minecraft/client/gui/Font;ZZIIZI)V"), index = 8)
    private boolean modifyChatScreenCommandSuggestionsAnchor(boolean bl) {
        if (!Config.INSTANCE.getValues().getEnabled() || Config.INSTANCE.getValues().getVanillaInputBox()) {
            return bl;
        }
        return Config.INSTANCE.getValues().getInputBoxSettings().renderBottom();
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
    private void keyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        if (ChatPlusScreenAdapter.INSTANCE.handleKeyPressed(thisScreen(), keyEvent)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;scrollChat(I)V", ordinal = 0))
    private void keyPressedPageUp(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        ChatPlusScreenAdapter.INSTANCE.handlePageUpDown(true);
    }

    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;scrollChat(I)V", ordinal = 1))
    private void keyPressedPageDown(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        ChatPlusScreenAdapter.INSTANCE.handlePageUpDown(false);
    }

    @Inject(method = "mouseScrolled", at = @At(value = "RETURN", ordinal = 1), cancellable = true)
    private void mouseScrolled(double mouseX, double mouseY, double amountX, double amountY, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        if (ChatPlusScreenAdapter.INSTANCE.handleMouseScrolled(thisScreen(), mouseX, mouseY, amountX, amountY)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @ModifyExpressionValue(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;)Z"))
    private boolean mouseClickedCommandSuggestions(boolean original, @Share("clicked") LocalBooleanRef booleanRef) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return original;
        }
        booleanRef.set(original);
        return original;
    }

    @Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;)Z", shift = At.Shift.AFTER), cancellable = true)
    private void mouseClickedAfter(MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfoReturnable<Boolean> cir, @Share("clicked") LocalBooleanRef booleanRef) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        if (booleanRef.get()) {
            return;
        }
        if (ChatPlusScreenAdapter.INSTANCE.handleMouseClicked(thisScreen(), mouseButtonEvent)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @ModifyExpressionValue(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;)Z"))
    private boolean mouseClickedEditBox(boolean original) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return original;
        }
        return input.isFocused() && original;
    }

    @Redirect(
            method = "mouseClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;captureClickableText(Lnet/minecraft/client/gui/ActiveTextCollector;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V"
            )
    )
    private void captureClickableText(ChatComponent instance, ActiveTextCollector activeTextCollector, int screenHeight, int ticks, ChatComponent.DisplayMode displayMode) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            this.minecraft.gui.hud.getChat().captureClickableText(activeTextCollector, screenHeight, ticks, displayMode);
        }
    }

    @Redirect(
            method = "mouseClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/ActiveTextCollector$ClickableStyleFinder;result()Lnet/minecraft/network/chat/Style;"
            )
    )
    private Style mouseClicked(ActiveTextCollector.ClickableStyleFinder instance, @Local(argsOnly = true) MouseButtonEvent mouseButtonEvent) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return instance.result();
        }
        return ChatManager.INSTANCE.getGlobalSelectedTab().getComponentStyleAt(mouseButtonEvent.x(), mouseButtonEvent.y());
    }


    @Inject(method = "moveInHistory", at = @At("HEAD"), cancellable = true)
    private void moveInHistory(int i, CallbackInfo ci) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        ChatPlusScreenAdapter.INSTANCE.handleMoveInHistory(thisScreen(), i);
        ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void renderHead(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        ChatPlusScreenAdapter.INSTANCE.handleRenderHead(thisScreen(), guiGraphics, mouseX, mouseY, partialTick);
    }

    @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"), index = 0)
    private int renderFillStartX(int x) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return x;
        }
        MovableChat.InputBoxSettings inputBoxSettings = Config.INSTANCE.getValues().getInputBoxSettings();
        if (LanguageManager.INSTANCE.getLanguageSpeakEnabled()) {
            if (Config.INSTANCE.getValues().getVanillaInputBox()) {
                return TranslateMessage.INSTANCE.getTRANSLATE_PREFIX_INPUT_WIDTH() + inputBoxSettings.getStartX();
            }
            return TranslateMessage.INSTANCE.getTRANSLATE_PREFIX_INPUT_WIDTH();
        } else {
            if (Config.INSTANCE.getValues().getVanillaInputBox()) {
                return x;
            }
            return inputBoxSettings.getStartX() - 2;
        }
    }

    @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"), index = 1)
    private int renderFillStartY(int y) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return y;
        }
        if (Config.INSTANCE.getValues().getVanillaInputBox()) {
            return y;
        }
        return Config.INSTANCE.getValues().getInputBoxSettings().getCalculatedStartY() - MovableChat.InputBoxSettings.INPUT_BOX_PADDING;
    }

    @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"), index = 2)
    private int renderFillWidth(int x) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return x;
        }
        if (Config.INSTANCE.getValues().getVanillaInputBox()) {
            return chatPlus$w;
        }
        return Config.INSTANCE.getValues().getInputBoxSettings().getStartX() + chatPlus$w;
    }

    @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"), index = 3)
    private int renderFillHeight(int y) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return y;
        }
        if (Config.INSTANCE.getValues().getVanillaInputBox()) {
            return y;
        }
        return Config.INSTANCE.getValues().getInputBoxSettings().getCalculatedStartY() + MovableChat.InputBoxSettings.PADDED_INPUT_BOX_HEIGHT;
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void renderTail(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        ChatPlusScreenAdapter.INSTANCE.handleRenderTail(thisScreen(), guiGraphics, mouseX, mouseY, partialTick);
    }

    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void handleChatInput(String string, boolean bl, CallbackInfo ci) {
        if (!Config.INSTANCE.getValues().getEnabled()) {
            return;
        }
        ChatPlusScreenAdapter.INSTANCE.handleChatInput(thisScreen(), string);
        ci.cancel();
    }

}
