package com.ebicep.chatplus.mixin;

import com.ebicep.chatplus.util.ComponentUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

@Mixin(Component.Serializer.class)
public class MixinComponentSerializer {

    @Inject(
            method = "serialize(Lnet/minecraft/network/chat/Component;Ljava/lang/reflect/Type;Lcom/google/gson/JsonSerializationContext;)Lcom/google/gson/JsonElement;",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/network/chat/Component;getContents()Lnet/minecraft/network/chat/ComponentContents;",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void instanceOfLiteralIgnored(
            Component component,
            Type type,
            JsonSerializationContext jsonSerializationContext,
            CallbackInfoReturnable<JsonElement> cir,
            @Local ComponentContents componentContents,
            @Local JsonObject jsonObject
    ) {
        if (componentContents instanceof ComponentUtil.LiteralContentsIgnored literalContentsIgnored) {
            jsonObject.addProperty("text", literalContentsIgnored.getText());
            cir.setReturnValue(jsonObject);
            cir.cancel();
        }
    }

//    @WrapOperation(
//            method = "serialize(Lnet/minecraft/network/chat/Component;Ljava/lang/reflect/Type;Lcom/google/gson/JsonSerializationContext;)Lcom/google/gson/JsonElement;",
//            constant = @Constant(classValue = LiteralContents.class)
//    )
//    private boolean idk(Object object, Operation<Boolean> original, @Local JsonObject jsonObject) {
//        if (object instanceof CompactMessages.LiteralContentsIgnored literalContentsIgnored) {
//            jsonObject.addProperty("text", literalContentsIgnored.getText());
//        }
//        return original.call(object);
//    }

}
