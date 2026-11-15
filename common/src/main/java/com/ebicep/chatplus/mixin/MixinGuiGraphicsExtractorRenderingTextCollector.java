package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.hud.ChatRenderContext;
import com.ebicep.chatplus.hud.ChatRenderContextHolder;
import com.ebicep.chatplus.hud.ChatRenderLineTextEvent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.client.gui.GuiGraphicsExtractor$RenderingTextCollector")
public abstract class MixinGuiGraphicsExtractorRenderingTextCollector {

    @ModifyArg(
            method = "accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/state/gui/GuiTextRenderState;<init>(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;Lorg/joml/Matrix3x2fc;IIIIZZLnet/minecraft/client/gui/navigation/ScreenRectangle;)V"
            ),
            index = 7
    )
    private boolean chatPlus$dropShadow(boolean dropShadow) {
        ChatRenderContext context = ChatRenderContextHolder.get();
        if (context == null) {
            return dropShadow;
        }
        @NotNull ChatRenderLineTextEvent event = context.getRenderTextLineEvent();
        return event.getChatWindow().getGeneralSettings().getTextShadow();
    }
}