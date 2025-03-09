package com.ebicep.chatplus.util

import com.ebicep.chatplus.hud.ChatScreenInputEvent
import com.ebicep.chatplus.hud.ChatScreenKeyPressedEvent
import com.ebicep.chatplus.hud.ChatScreenMouseClickedEvent
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

object KeyUtil {

    fun InputConstants.Key.isDown(): Boolean {
        if (value == -1) return false
        val window = Minecraft.getInstance().window ?: return false
        return if (isMouseButton(value)) {
            GLFW.glfwGetMouseButton(window.window, value) == GLFW.GLFW_PRESS
        } else {
            InputConstants.isKeyDown(window.window, value)
        }
    }

    fun InputConstants.Key.isDown(event: ChatScreenInputEvent): Boolean {
        val inputEvent = event.inputEvent
        when (inputEvent) {
            is ChatScreenMouseClickedEvent -> {
                val button = inputEvent.button
                return isMouseButton(button) && button == value
            }

            is ChatScreenKeyPressedEvent -> {
                return inputEvent.keyCode == value
            }

            else -> {
                return false
            }
        }
    }

    fun InputConstants.Key.isAlt(): Boolean = value == InputConstants.KEY_LALT || value == InputConstants.KEY_RALT

    fun isAlt(value: Int): Boolean = value == InputConstants.KEY_LALT || value == InputConstants.KEY_RALT

    fun InputConstants.Key.isControl(): Boolean =
        if (Minecraft.ON_OSX) value == InputConstants.KEY_LWIN || value == InputConstants.KEY_RWIN
        else value == InputConstants.KEY_LCONTROL || value == InputConstants.KEY_RCONTROL

    fun isControl(value: Int): Boolean =
        if (Minecraft.ON_OSX) value == InputConstants.KEY_LWIN || value == InputConstants.KEY_RWIN
        else value == InputConstants.KEY_LCONTROL || value == InputConstants.KEY_RCONTROL

    fun InputConstants.Key.isShift(): Boolean = value == InputConstants.KEY_LSHIFT || value == InputConstants.KEY_RSHIFT

    fun isShift(value: Int): Boolean = value == InputConstants.KEY_LSHIFT || value == InputConstants.KEY_RSHIFT

    fun InputConstants.Key.isMouseButton(): Boolean = isMouseButton(value)

    fun isMouseButton(value: Int): Boolean = value in 0..7

    fun InputConstants.Key.isModifier(): Boolean = isAlt() || isControl() || isShift()

    fun isModifier(value: Int): Boolean = isAlt(value) || isControl(value) || isShift(value)


    fun InputConstants.Key.getDisplayName(parentheses: Boolean): Component {
        if (this.value == -1) {
            return Component.empty()
        }
        val keyDisplayName = displayName
        val component = Component.empty()
        if (parentheses) {
            component.append(Component.literal(" ("))
        }
        component.append(keyDisplayName)
        if (parentheses) {
            component.append(Component.literal(")"))
        }
        return component
    }


}