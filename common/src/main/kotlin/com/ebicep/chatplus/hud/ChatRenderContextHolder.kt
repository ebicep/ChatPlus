package com.ebicep.chatplus.hud

object ChatRenderContextHolder {

    private val current = ThreadLocal<ChatRenderContext?>()

    @JvmStatic
    fun get(): ChatRenderContext? = current.get()

    @JvmStatic
    fun set(context: ChatRenderContext?) {
        if (context == null) {
            current.remove()
        } else {
            current.set(context)
        }
    }
}