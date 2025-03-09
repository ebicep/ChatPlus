package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.IChatScreen
import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.config.Config.values
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.FindMessage
import com.ebicep.chatplus.features.MovableChat.InputBoxSettings.Companion.INPUT_BOX_PADDING
import com.ebicep.chatplus.features.internal.Debug
import com.ebicep.chatplus.hud.ChatPlusScreen.EDIT_BOX_HEIGHT
import com.ebicep.chatplus.hud.ChatScreenCloseEvent
import com.ebicep.chatplus.hud.ChatScreenInitPreEvent
import com.ebicep.chatplus.hud.ChatScreenMouseClickedEvent
import com.ebicep.chatplus.hud.ChatScreenRenderEvent
import net.minecraft.client.gui.screens.ChatScreen

object TextBarElements {

    const val PADDING = 6
    private const val SPACER = 1 // space between text box / find / translate

    private lateinit var chatPlusScreen: ChatScreen
    private var textBarElements: MutableList<TextBarElement> = mutableListOf()
    private var textBarElementsStartX: MutableMap<TextBarElement, Int> = mutableMapOf()

    init {
        EventBus.register<ChatScreenInitPreEvent> {
            chatPlusScreen = it.screen
            if (textBarElements.isEmpty()) {
                EventBus.post(AddTextBarElementEvent(chatPlusScreen, textBarElements))
            }

            //____TEXTBOX_____-FIND--TRANSLATE-
            textBarElements.forEach { element ->
                element.init()
                val calculatedWidth = element.getPaddedWidth() + SPACER
                (chatPlusScreen as IChatScreen).chatPlusWidth -= calculatedWidth
            }
            if (textBarElements.isNotEmpty() && Config.values.vanillaInputBox) {
                (chatPlusScreen as IChatScreen).chatPlusWidth -= 2
            }
            cacheTextBarElementXs()
        }
        EventBus.register<ChatScreenCloseEvent> {
            textBarElements.clear()
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            val mouseX = it.mouseX
            val mouseY = it.mouseY
            val height = chatPlusScreen.height
            val currentY = if (values.vanillaInputBox) height - EDIT_BOX_HEIGHT else values.inputBoxSettings.getCalculatedStartY() - INPUT_BOX_PADDING
            textBarElements.forEach { element ->
                val x = textBarElementsStartX[element]!!
                if (x < mouseX &&
                    mouseX < x + element.getPaddedWidth() &&
                    currentY < mouseY &&
                    mouseY < currentY + EDIT_BOX_HEIGHT
                ) {
                    element.onClick(it.button)
                }
                element.onClickEvent(it)
            }
        }
        EventBus.register<ChatScreenRenderEvent> {
            val guiGraphics = it.guiGraphics
            val mouseX = it.mouseX
            val mouseY = it.mouseY
            val partialTick = it.partialTick
            val height = chatPlusScreen.height
            val currentY = if (values.vanillaInputBox) height - EDIT_BOX_HEIGHT else values.inputBoxSettings.getCalculatedStartY() - INPUT_BOX_PADDING
            if (Debug.debug) {
                guiGraphics.fill(
                    0,
                    currentY,
                    5,
                    currentY + EDIT_BOX_HEIGHT,
                    0xAF00FF00.toInt()
                )
            }
            textBarElements.forEach { element ->
                val elementStartX = textBarElementsStartX[element]!!
                element.onRender(guiGraphics, elementStartX, currentY, mouseX, mouseY, partialTick)
                if (elementStartX < mouseX &&
                    mouseX < elementStartX + element.getPaddedWidth() &&
                    currentY < mouseY &&
                    mouseY < currentY + EDIT_BOX_HEIGHT
                ) {
                    element.onHover(guiGraphics, mouseX, mouseY)
                }
            }
        }

        FindMessage
    }

    private fun cacheTextBarElementXs() {
        var currentX = (chatPlusScreen as IChatScreen).chatPlusWidth + SPACER
        textBarElements.forEach {
            textBarElementsStartX[it] = currentX
            currentX += it.getPaddedWidth() + SPACER
        }
    }

}

data class AddTextBarElementEvent(
    val screen: ChatScreen,
    val elements: MutableList<TextBarElement>
) : Event

