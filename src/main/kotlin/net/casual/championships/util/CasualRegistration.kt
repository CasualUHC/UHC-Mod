package net.casual.championships.util

import net.casual.arcade.minigame.utils.MinigameRegistries
import net.casual.arcade.utils.codec.CodecProvider.Companion.register
import net.casual.championships.minigame.CasualChampionshipsTemplate

object CasualRegistration {
    fun register() {
        CasualChampionshipsTemplate.register(MinigameRegistries.MINIGAMES_EVENT)
    }
}