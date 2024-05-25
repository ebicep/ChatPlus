package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.IChatScreen
import com.ebicep.chatplus.events.Event
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.FindMessage
import com.ebicep.chatplus.hud.ChatPlusScreen.EDIT_BOX_HEIGHT
import com.ebicep.chatplus.hud.ChatScreenCloseEvent
import com.ebicep.chatplus.hud.ChatScreenInitPreEvent
import com.ebicep.chatplus.hud.ChatScreenMouseClickedEvent
import com.ebicep.chatplus.hud.ChatScreenRenderEvent
import net.minecraft.client.gui.screens.ChatScreen

object TextBarElements {

    const val PADDING = 6
    const val SPACER = 2 // space between text box / find / translate

    data class AddTextBarElementEvent(
        val screen: ChatScreen,
        val elements: MutableList<TextBarElement>
    ) : Event

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
                val calculatedWidth = element.getPaddedWidth() + SPACER
                (chatPlusScreen as IChatScreen).chatPlusWidth -= calculatedWidth
            }
            cacheTextBarElementXs()
        }
        EventBus.register<ChatScreenCloseEvent> {
            textBarElements.clear()
        }
        EventBus.register<ChatScreenMouseClickedEvent> {
            if (it.button != 0) {
                return@register
            }
            val mouseX = it.mouseX
            val mouseY = it.mouseY
            val height = chatPlusScreen.height
            textBarElements.forEach { element ->
                val x = textBarElementsStartX[element]!!
                if (x < mouseX &&
                    mouseX < x + element.getPaddedWidth() &&
                    height - EDIT_BOX_HEIGHT < mouseY &&
                    mouseY < height
                ) {
                    element.onClick()
                }
            }
        }
        EventBus.register<ChatScreenRenderEvent> {
            val guiGraphics = it.guiGraphics
            val mouseX = it.mouseX
            val mouseY = it.mouseY
            val height = chatPlusScreen.height
            val currentY = height - EDIT_BOX_HEIGHT
            textBarElements.forEach { element ->
                val elementStartX = textBarElementsStartX[element]!!
                element.onRender(guiGraphics, elementStartX, currentY, mouseX, mouseY)
                if (elementStartX < mouseX &&
                    mouseX < elementStartX + element.getPaddedWidth() &&
                    height - EDIT_BOX_HEIGHT < mouseY &&
                    mouseY < height
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
