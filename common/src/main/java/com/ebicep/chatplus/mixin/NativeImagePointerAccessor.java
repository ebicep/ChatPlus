package com.ebicep.chatplus.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Modified from
 * <a href="https://github.com/comp500/ScreenshotToClipboard">ScreenshotToClipboard</a> and
 */
@Mixin(NativeImage.class)
public interface NativeImagePointerAccessor {
    @Accessor("size")
    long size();

}