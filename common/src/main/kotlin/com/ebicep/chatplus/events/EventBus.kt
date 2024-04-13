package com.ebicep.chatplus.events

interface Event

object EventBus {

    private val bus = mutableMapOf<String, Bus<*>>()

    @Suppress("UNCHECKED_CAST")
    private class Bus<T> {

        private val subscribers = mutableListOf<EventData<T>>()

        fun <E> register(priority: Int, skipOtherCallbacks: () -> Boolean, callback: (E) -> Unit) {
            subscribers.add(EventData(priority, skipOtherCallbacks, callback as (T) -> Unit))
            // higher priority first
            subscribers.sortByDescending { it.priority }
        }

        fun <E> unregister(callback: (E) -> Unit) {
            subscribers.removeIf { it.callback == callback }
        }

        fun <E> post(data: E): E {
            for (it in subscribers) {
                it.callback.invoke(data as T)
                if (it.skipOtherCallbacks()) {
                    break
                }
            }
            return data
        }

        data class EventData<T>(
            val priority: Int,
            val skipOtherCallbacks: () -> Boolean = { false },
            val callback: (T) -> Unit
        )

    }

    /**
     * Allows EventBus.register<Event Class>({ })
     */
    inline fun <reified T> register(
        priority: Int = 0,
        noinline skipOtherCallbacks: () -> Boolean = { false },
        noinline callback: (T) -> Unit
    ) = register(priority, skipOtherCallbacks, T::class.java, callback)

    inline fun <reified T> unregister(noinline callback: (T) -> Unit) =
        unregister(T::class.java, callback)

    inline fun <reified T> post(data: T) =
        post(T::class.java, data)

    fun <T> register(priority: Int, skipOtherCallbacks: () -> Boolean, clazz: Class<T>, callback: (T) -> Unit) {
        if (!bus.containsKey(clazz.toString())) {
            bus[clazz.toString()] = Bus<T>()
        }

        bus[clazz.toString()]!!.register(priority, skipOtherCallbacks, callback)
    }

    @Throws(NoSuchElementException::class)
    fun <T> unregister(clazz: Class<T>, callback: (T) -> Unit) {
        if (!bus.containsKey(clazz.toString())) {
            throw NoSuchElementException("Can't find Bus for event ${clazz}?")
        }

        bus[clazz.toString()]!!.unregister(callback)
    }

    fun <T> post(clazz: Class<T>, data: T): T {
        if (!bus.containsKey(clazz.toString())) {
            bus[clazz.toString()] = Bus<T>()
        }

        return bus[clazz.toString()]!!.post(data)
    }


}
