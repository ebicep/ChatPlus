package com.ebicep.chatplus.mixin;

import net.minecraft.client.StringSplitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StringSplitter.class)
public interface IMixinStringSplitter {

    @Accessor("widthProvider")
    StringSplitter.WidthProvider callGetWidthProvider();

}
