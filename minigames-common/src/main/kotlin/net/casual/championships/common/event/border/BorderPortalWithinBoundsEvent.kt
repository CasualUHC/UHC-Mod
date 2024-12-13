package net.casual.championships.common.event.border

import net.casual.arcade.events.common.CancellableEvent
import net.casual.arcade.events.server.level.LevelEvent
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.border.WorldBorder

data class BorderPortalWithinBoundsEvent(
    val border: WorldBorder,
    override val level: ServerLevel,
    val pos: BlockPos
): CancellableEvent.Typed<Boolean>(), LevelEvent