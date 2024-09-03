package com.ebicep.chatplus.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphics.class)
public interface IMixinGuiGraphics {

    @Accessor("bufferSource")
    MultiBufferSource.BufferSource getBufferSource();

    @Invoker("flushIfUnmanaged")
    void callFlushIfUnmanaged();

}
