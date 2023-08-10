package com.ebicep.chatplus.mixin;

import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Style.class)
public interface MixinStyle {

    @Accessor("hoverEvent")
    void setHoverEvent(HoverEvent hoverEvent);

}
