package com.ebicep.chatplus.mixin;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatScreen.class)
public interface IMixinChatScreen {

    @Accessor("historyBuffer")
    String getHistoryBuffer();

    @Accessor("historyBuffer")
    void setHistoryBuffer(String historyBuffer);

    @Accessor("historyPos")
    int getHistoryPos();

    @Accessor("historyPos")
    void setHistoryPos(int historyPos);

    @Accessor("input")
    EditBox getInput();

    @Accessor("initial")
    String getInitial();

    @Accessor("initial")
    void setInitial(String initial);

    @Accessor("commandSuggestions")
    CommandSuggestions getCommandSuggestions();

}
