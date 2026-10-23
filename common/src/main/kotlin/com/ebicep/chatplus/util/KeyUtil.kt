package com.ebicep.chatplus.util

import com.ebicep.chatplus.hud.ChatScreenInputEvent
import com.ebicep.chatplus.hud.ChatScreenKeyPressedEvent
import com.ebicep.chatplus.hud.ChatScreenMouseClickedEvent
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object KeyUtil {

    fun InputConstants.Key.isDown(): Boolean {
        if (value == -1) return false
        val minecraft = Minecraft.getInstance()
        return if (isMouseButton(value)) {
            val mouseHandler = minecraft.mouseHandler
            when (value) {
                InputConstants.MOUSE_BUTTON_LEFT -> mouseHandler.isLeftPressed
                InputConstants.MOUSE_BUTTON_RIGHT -> mouseHandler.isRightPressed
                InputConstants.MOUSE_BUTTON_MIDDLE -> mouseHandler.isMiddlePressed
                else -> InputConstants.isKeyDown(value)
            }
        } else {
            InputConstants.isKeyDown(value)
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
                return inputEvent.keyCode == value || inputEvent.scanCode == value
            }

            else -> {
                return false
            }
        }
    }

    fun InputConstants.Key.isAlt(): Boolean = value == InputConstants.KEY_LALT || value == InputConstants.KEY_RALT

    fun isAlt(value: Int): Boolean = value == InputConstants.KEY_LALT || value == InputConstants.KEY_RALT

    fun InputConstants.Key.isControl(): Boolean =
        value == InputConstants.KEY_LCONTROL || value == InputConstants.KEY_RCONTROL

    fun isControl(value: Int): Boolean =
        value == InputConstants.KEY_LCONTROL || value == InputConstants.KEY_RCONTROL

    fun InputConstants.Key.isShift(): Boolean = value == InputConstants.KEY_LSHIFT || value == InputConstants.KEY_RSHIFT

    fun isShift(value: Int): Boolean = value == InputConstants.KEY_LSHIFT || value == InputConstants.KEY_RSHIFT

    fun InputConstants.Key.isMouseButton(): Boolean = isMouseButton(value)

    fun isMouseButton(value: Int): Boolean =
        value == InputConstants.MOUSE_BUTTON_LEFT
                || value == InputConstants.MOUSE_BUTTON_RIGHT
                || value == InputConstants.MOUSE_BUTTON_MIDDLE
                || value == InputConstants.MOUSE_BUTTON_4
                || value == InputConstants.MOUSE_BUTTON_5
                || value == InputConstants.MOUSE_BUTTON_6
                || value == InputConstants.MOUSE_BUTTON_7
                || value == InputConstants.MOUSE_BUTTON_8

    fun isLeftMouseButton(button: Int): Boolean = button == InputConstants.MOUSE_BUTTON_LEFT

    fun isRightMouseButton(button: Int): Boolean = button == InputConstants.MOUSE_BUTTON_RIGHT

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