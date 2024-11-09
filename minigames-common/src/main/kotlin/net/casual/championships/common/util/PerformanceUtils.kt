package net.casual.championships.common.util

import net.casual.arcade.dimensions.utils.setCustomMobSpawningRules
import net.casual.arcade.events.entity.EntityStartTrackingEvent
import net.casual.arcade.minigame.Minigame
import net.casual.championships.common.level.ReducedMobSpawningRules
import net.minecraft.world.entity.EntityType.*
import net.minecraft.world.entity.Mob

object PerformanceUtils {
    private val DISABLED_ENTITIES = setOf(PIG, COW, SQUID, BAT, GLOW_SQUID, FOX, COD, SALMON, PARROT, MOOSHROOM, SKELETON_HORSE, CAT, CHICKEN, SHEEP, GOAT, FROG)

    fun reduceMinigameMobcap(minigame: Minigame) {
        for (level in minigame.levels.all()) {
            level.setCustomMobSpawningRules(ReducedMobSpawningRules)
        }
    }

    fun disableEntityAI(minigame: Minigame) {
        minigame.events.register<EntityStartTrackingEvent> {
            val entity = it.entity
            if (entity is Mob && DISABLED_ENTITIES.contains(entity.type)) {
                entity.isNoAi = true
            }
        }
    }
}