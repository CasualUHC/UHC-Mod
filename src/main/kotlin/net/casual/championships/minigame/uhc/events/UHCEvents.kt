package net.casual.championships.minigame.uhc.events

import net.casual.championships.util.Config

object UHCEvents {
    private val events = HashMap<String, () -> UHCEvent>()

    private val event by Config.string("event", "default")

    fun getUHC(name: String = event): UHCEvent {
        return events[name]?.invoke() ?: DefaultUHC
    }

    fun register(name: String, factory: () -> UHCEvent) {
        events[name] = factory
    }
}