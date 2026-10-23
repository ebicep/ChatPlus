package com.ebicep.chatplus.config.serializers

import com.ebicep.chatplus.hud.ChatScreenInputEvent
import com.ebicep.chatplus.hud.ChatScreenKeyPressedEvent
import com.ebicep.chatplus.hud.ChatScreenMouseClickedEvent
import com.ebicep.chatplus.util.KeyUtil
import com.ebicep.chatplus.util.KeyUtil.isDown
import com.ebicep.chatplus.util.KeyUtil.isModifier
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.serialization.Serializable
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

@Serializable
data class KeyWithModifier(
    @Serializable(with = KeySerializer::class)
    var key: InputConstants.Key,
    var modifier: Short
) {

    fun isDown(): Boolean {
        return key.isDown() && modifierSatisfied(modifier)
    }

    fun isDown(keyCode: Int, eventModifier: Int): Boolean {
        return !(key.value == -1 || !keyMatchesEventKey(keyCode)) && (modifierSatisfied(modifier) || legacyEventModifierMatches(eventModifier))
    }

    private fun keyMatchesEventKey(eventKey: Int): Boolean = key.value == eventKey

    private fun keyMatchesChatKey(event: ChatScreenKeyPressedEvent): Boolean {
        return key.value == event.keyCode || key.value == event.scanCode
    }

    private fun modifierSatisfied(required: Short): Boolean {
        if (required == 0.toShort()) {
            return true
        }
        val minecraft = Minecraft.getInstance()
        return when (required.toInt()) {
            1 -> minecraft.hasAltDown()
            2 -> minecraft.hasControlDown()
            4 -> minecraft.hasShiftDown()
            else -> false
        }
    }

    private fun legacyEventModifierMatches(inputModifier: Int): Boolean {
        val keyModifier = modifier.toInt()
        if (key.isModifier()) {
            return when (keyModifier) { // when key is a modifier and has modifier
                1 -> inputModifier == 5 || inputModifier == 6 // alt
                2 -> inputModifier == 3 || inputModifier == 6 // control
                4 -> inputModifier == 3 || inputModifier == 5 // shift
                else -> inputModifier == keyModifier
            }
        }
        return inputModifier == keyModifier || keyModifier == 2 && inputModifier == 3
    }

    fun isDown(event: ChatScreenInputEvent): Boolean {
        val inputEvent = event.inputEvent
        when (inputEvent) {
            is ChatScreenMouseClickedEvent -> {
                val button = inputEvent.button
                val mouseDown = KeyUtil.isMouseButton(button) && button == key.value
                return mouseDown && modifierSatisfied(modifier)
            }

            is ChatScreenKeyPressedEvent -> {
                val keyDown = keyMatchesChatKey(inputEvent)
                val modifierDown = modifierSatisfied(modifier) || legacyEventModifierMatches(inputEvent.modifiers)
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