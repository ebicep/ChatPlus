package com.ebicep.chatplus.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface IMixinScreen {

    @Invoker("setInitialFocus")
    void callSetInitialFocus(GuiEventListener guiEventListener);

    @Accessor("font")
    Font getFont();

    @Invoker("addWidget")
    <T extends GuiEventListener> T callAddWidget(T guiEventListener);

    @Invoker("rebuildWidgets")
    void callRebuildWidgets();

}
