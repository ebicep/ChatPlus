package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.config.Config;
import com.ebicep.chatplus.config.ConfigVariables;
import com.ebicep.chatplus.features.MovableChat;
import com.ebicep.chatplus.features.chattabs.ServerChatTabSettings;
import com.ebicep.chatplus.hud.ChatManager;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandSuggestions.class)
public class MixinCommandSuggestions {

    @Shadow
    @Final
    EditBox input;

    @Inject(
            method = "showSuggestions",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/suggestion/Suggestions;isEmpty()Z"
            )
    )
    private void showSuggestions(boolean bl, CallbackInfo ci, @Local @NotNull Suggestions suggestions) {
        ConfigVariables values = Config.INSTANCE.getValues();
        if (!values.getEnabled()) {
            return;
        }
        ServerChatTabSettings currentSettings = ChatManager.INSTANCE.getGlobalSelectedTab().getCurrentSettings();
        currentSettings.modifyCommandSuggestions(this.input.getValue(), suggestions.getList());
    }

    @ModifyVariable(
            method = "showSuggestions",
            at = @At(value = "STORE"),
            ordinal = 2
    )
    private int modifySuggestionY(int i) {
        ConfigVariables values = Config.INSTANCE.getValues();
        if (!values.getEnabled() || values.getVanillaInputBox()) {
            return i;
        }
        boolean bottom = Config.INSTANCE.getValues().getInputBoxSettings().renderBottom();
        return bottom ?
               values.getInputBoxSettings().getCalculatedStartY() :
               values.getInputBoxSettings().getCalculatedStartY() + MovableChat.InputBoxSettings.PADDED_INPUT_BOX_HEIGHT;
    }

    @ModifyVariable(
            method = "renderUsage",
            at = @At(value = "STORE"),
            ordinal = 1
    )
    private int modifyRenderUsageY(int j, @Local(ordinal = 0) int i) {
        ConfigVariables values = Config.INSTANCE.getValues();
        if (!values.getEnabled() || values.getVanillaInputBox()) {
            return j;
        }
        boolean bottom = Config.INSTANCE.getValues().getInputBoxSettings().renderBottom();
        return bottom ?
               values.getInputBoxSettings().getCalculatedStartY() - 14 - 12 * i :
               values.getInputBoxSettings().getCalculatedStartY() + MovableChat.InputBoxSettings.PADDED_INPUT_BOX_HEIGHT + 12 * i;
    }

}
