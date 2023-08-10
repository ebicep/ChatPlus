package com.ebicep.chatplus.config

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

object ChatPlusKeyBindings {

    val NO_SCOLL: ChatPlusKeyMapping = ChatPlusKeyMapping("key.noScroll", GLFW.GLFW_KEY_LEFT_CONTROL)
    val FINE_SCROLL: ChatPlusKeyMapping = ChatPlusKeyMapping("key.fineScroll", GLFW.GLFW_KEY_LEFT_SHIFT)
    val LARGE_SCROLL: ChatPlusKeyMapping = ChatPlusKeyMapping("key.largeScroll", GLFW.GLFW_KEY_LEFT_ALT)
    val MOVE_CHAT: ChatPlusKeyMapping = ChatPlusKeyMapping("key.moveChat", GLFW.GLFW_KEY_RIGHT_CONTROL)
    val COPY_MESSAGE: ChatPlusKeyMapping = ChatPlusKeyMapping("key.copyMessage", GLFW.GLFW_KEY_C)
    val COPY_MESSAGE_MODIFIER: ChatPlusKeyMapping = ChatPlusKeyMapping("key.copyMessageModifier", GLFW.GLFW_KEY_LEFT_CONTROL)
    val KEY_BINDINGS = arrayOf(NO_SCOLL, FINE_SCROLL, LARGE_SCROLL, MOVE_CHAT, COPY_MESSAGE, COPY_MESSAGE_MODIFIER)

    class ChatPlusKeyMapping(name: String, keyCode: Int) :
        KeyMapping(name, InputConstants.Type.KEYSYM, keyCode, "key.categories.chatPlus") {

        override fun same(pBinding: KeyMapping): Boolean {
            if (pBinding !is ChatPlusKeyMapping) {
                return false
            }
            return super.same(pBinding)
        }

    }

}