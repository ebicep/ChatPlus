package com.ebicep.chatplus.config.serializers

import com.ebicep.chatplus.hud.ChatScreenInputEvent
import com.ebicep.chatplus.hud.ChatScreenKeyPressedEvent
import com.ebicep.chatplus.hud.ChatScreenMouseClickedEvent
import com.ebicep.chatplus.util.KeyUtil
import com.ebicep.chatplus.util.KeyUtil.isDown
import com.ebicep.chatplus.util.KeyUtil.isModifier
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.Serializable
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

@Serializable
data class KeyWithModifier(
    @Serializable(with = KeySerializer::class)
    var key: InputConstants.Key,
    var modifier: Short
) {

    fun isDown(): Boolean {
        val keyDown = key.isDown()
        val modifierDown = modifier == 0.toShort() ||
                modifier == 1.toShort() && Screen.hasAltDown() ||
                modifier == 2.toShort() && Screen.hasControlDown() ||
                modifier == 4.toShort() && Screen.hasShiftDown()
        return keyDown && modifierDown
    }

    fun isDown(keyCode: Int, modifier: Int): Boolean {
        return key.value != -1 && key.value == keyCode && (this.modifier == 0.toShort() || this.modifier == modifier.toShort())
    }

    fun isDown(event: ChatScreenInputEvent): Boolean {
        val inputEvent = event.inputEvent
        when (inputEvent) {
            is ChatScreenMouseClickedEvent -> {
                val button = inputEvent.button
                val mouseDown = KeyUtil.isMouseButton(button) && button == key.value
                val modifierDown = modifier == 0.toShort() ||
                        modifier == 1.toShort() && Screen.hasAltDown() ||
                        modifier == 2.toShort() && Screen.hasControlDown() ||
                        modifier == 4.toShort() && Screen.hasShiftDown()
                return mouseDown && modifierDown
            }

            is ChatScreenKeyPressedEvent -> {
                val inputModifier = inputEvent.modifiers
                val keyDown = inputEvent.keyCode == key.value
                val keyModifier = modifier.toInt()
                val modifierDown = keyModifier == 0 ||
                        if (key.isModifier()) { // when key is a modifier and has modifier
                            when (keyModifier) {
                                1 -> inputModifier == 5 || inputModifier == 6 // alt
                                2 -> inputModifier == 3 || inputModifier == 6 // control
                                4 -> inputModifier == 3 || inputModifier == 5 // shift
                                else -> inputEvent.modifiers == keyModifier
                            }
                        } else {
                            inputModifier == keyModifier || keyModifier == 2 && inputModifier == 3 // allow for shift + control + key
                        }
                return keyDown && modifierDown
            }

            else -> {
                return false
            }
        }
    }

    fun getDisplayName(parentheses: Boolean): Component {
        if (key.value == -1) {
            return Component.empty()
        }
        val modifierDisplayName = when (modifier) {
            1.toShort() -> "Alt"
            2.toShort() -> "Ctrl"
            4.toShort() -> "Shift"
            else -> ""
        }
        val keyDisplayName = key.displayName
        val component = Component.empty()
        if (parentheses) {
            component.append(Component.literal(" ("))
        }
        component.append(Component.literal(modifierDisplayName))
        if (modifierDisplayName.isNotEmpty()) {
            component.append(Component.literal(" + "))
        }
        component.append(keyDisplayName)
        if (parentheses) {
            component.append(Component.literal(")"))
        }
        return component
    }

}