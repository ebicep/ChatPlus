package com.ebicep.chatplus.config

import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt

/**
 * Defines which screen corner/edge the chat window's position is anchored to.
 *
 * The stored renderer.x and renderer.y values are always anchor-relative distances:
 *   LEFT  anchors:  x = positive pixels from left edge of screen to left edge of window
 *   RIGHT anchors:  x = negative pixels (i.e. -distRight), so internalX = screenW + x - internalW
 *   TOP   anchors:  y = positive pixels from top edge of screen to top edge of window
 *   BOTTOM anchors: y = negative pixels (i.e. -distBottom), so internalY = screenH + y
 *   CENTER:         x/y = signed pixel offset from screen center to window center
 *
 * The default anchor is BOTTOM_LEFT, matching Minecraft's default chat position (y = -30 = 30px from bottom).
 */
@Serializable
enum class AnchorPoint(val key: String) : EnumTranslatableName {
    TOP_LEFT("chatPlus.chatWindow.generalSettings.anchorPoint.topLeft"),
    TOP_RIGHT("chatPlus.chatWindow.generalSettings.anchorPoint.topRight"),
    BOTTOM_LEFT("chatPlus.chatWindow.generalSettings.anchorPoint.bottomLeft"),
    BOTTOM_RIGHT("chatPlus.chatWindow.generalSettings.anchorPoint.bottomRight"),
    CENTER("chatPlus.chatWindow.generalSettings.anchorPoint.center"),

    ;

    val translatable: Component = Component.translatable(key)

    override fun getTranslatableName(): Component = translatable

    /**
     * Converts anchor-relative x (stored in renderer.x) to the absolute internalX (left edge of window).
     *   LEFT:   x = distLeft  → internalX = x
     *   RIGHT:  x = -distRight → internalX = screenWidth + x - internalWidth
     *   CENTER: x = centerOffset → internalX = screenWidth/2 + x - internalWidth/2
     */
    fun anchorXToAbsolute(x: Int, internalWidth: Int, screenWidth: Int): Int {
        return when (this) {
            TOP_LEFT, BOTTOM_LEFT -> x
            TOP_RIGHT, BOTTOM_RIGHT -> screenWidth + x - internalWidth
            CENTER -> (screenWidth / 2.0 + x - internalWidth / 2.0).roundToInt()
        }
    }

    /**
     * Converts anchor-relative y (stored in renderer.y) to the absolute internalY (bottom edge of window).
     *   TOP:    y = distTop   → internalY = y + internalHeight  (bottom = top + height)
     *   BOTTOM: y = -distBottom → internalY = screenHeight + y
     *   CENTER: y = centerOffset → internalY = screenHeight/2 + y + internalHeight/2
     */
    fun anchorYToAbsolute(y: Int, internalHeight: Int, screenHeight: Int): Int {
        return when (this) {
            TOP_LEFT, TOP_RIGHT -> y + internalHeight
            BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight + y
            CENTER -> (screenHeight / 2.0 + y + internalHeight / 2.0).roundToInt()
        }
    }

    /**
     * Converts an absolute internalX (left edge) to the anchor-relative x for this anchor.
     * Inverse of anchorXToAbsolute. Used when the player moves/places the window.
     */
    fun absoluteToAnchorX(absoluteX: Int, screenWidth: Int, internalWidth: Int): Int {
        return when (this) {
            TOP_LEFT, BOTTOM_LEFT -> absoluteX
            TOP_RIGHT, BOTTOM_RIGHT -> absoluteX + internalWidth - screenWidth
            // Group the non-absoluteX terms in a single round to guarantee a lossless round-trip
            // through anchorXToAbsolute for any integer absoluteX, even when internalWidth is odd.
            CENTER -> absoluteX - (screenWidth / 2.0 - internalWidth / 2.0).roundToInt()
        }
    }

    /**
     * Converts an absolute internalY (bottom edge) to the anchor-relative y for this anchor.
     * Inverse of anchorYToAbsolute. Used when the player moves/places the window.
     */
    fun absoluteToAnchorY(absoluteY: Int, screenHeight: Int, internalHeight: Int): Int {
        return when (this) {
            TOP_LEFT, TOP_RIGHT -> absoluteY - internalHeight
            BOTTOM_LEFT, BOTTOM_RIGHT -> absoluteY - screenHeight
            // Group the non-absoluteY terms in a single round to guarantee a lossless round-trip
            // through anchorYToAbsolute for any integer absoluteY, even when internalHeight is odd.
            CENTER -> absoluteY - (internalHeight / 2.0 + screenHeight / 2.0).roundToInt()
        }
    }
}
