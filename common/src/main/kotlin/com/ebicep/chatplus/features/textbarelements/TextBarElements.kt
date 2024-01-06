package com.ebicep.chatplus.features.textbarelements

import com.ebicep.chatplus.config.Config
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.hud.*

object TextBarElements {

    private lateinit var chatPlusScreen: ChatPlusScreen
    private var textBarElements = mutableListOf<TextBarElement>()
    private var textBarElementsStartX: MutableMap<TextBarElement, Int> = mutableMapOf()

    init {
        EventBus.register<ChatScreenInitEvent> {
            if (textBarElements.isNotEmpty()) {
                return@register
            }
            chatPlusScreen = it.screen
            textBarElements.add(FindTextBarElement(chatPlusScreen))
            if (Config.values.translatorEnabled) {
                textBarElements.add(TranslateSpeakTextBarElement(chatPlusScreen))
            }

            //____TEXTBOX_____-FIND--TRANSLATE-
            textBarElements.forEach { element ->
                val calculatedWidth = element.getPaddedWidth() + SPACER
                chatPlusScreen.editBoxWidth -= calculatedWidth
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
    }

    private fun cacheTextBarElementXs() {
        var currentX = chatPlusScreen.editBoxWidth + SPACER
        textBarElements.forEach {
            textBarElementsStartX[it] = currentX
            currentX += it.getPaddedWidth() + SPACER
        }
    }


}