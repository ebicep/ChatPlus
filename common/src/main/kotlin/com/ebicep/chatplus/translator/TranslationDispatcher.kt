package com.ebicep.chatplus.translator

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Runs translation work off the Minecraft client thread.
 * Provider HTTP is blocking and must only be invoked from tasks submitted here.
 */
object TranslationDispatcher {

    private val threadIndex = AtomicInteger()

    private val executor = Executors.newFixedThreadPool(
        4,
    ) { runnable ->
        Thread(runnable, "chatplus-translate-${threadIndex.incrementAndGet()}").apply { isDaemon = true }
    }

    fun submit(task: Runnable) {
        executor.execute(task)
    }
}