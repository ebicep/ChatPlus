package com.ebicep.chatplus.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphics.class)
public interface IMixinGuiGraphics {

    @Invoker("innerBlit")
    void callInnerBlit(RenderPipeline renderPipeline, Identifier identifier, int i, int j, int k, int l, float f, float g, float h, float m, int n);

}
