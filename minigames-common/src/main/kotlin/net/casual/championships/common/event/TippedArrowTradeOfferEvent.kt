package net.casual.championships.common.event

import net.casual.arcade.events.server.level.LevelEvent
import net.minecraft.core.Holder
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.alchemy.Potion

data class TippedArrowTradeOfferEvent(
    val trader: Entity,
    var potion: Holder<Potion>,
): LevelEvent {
    override val level: ServerLevel
        get() = this.trader.level() as ServerLevel
}