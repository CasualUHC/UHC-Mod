package net.casual.championships.common.event.border

import net.casual.arcade.events.common.CancellableEvent
import net.casual.arcade.events.server.level.LevelEvent
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.border.WorldBorder
import net.minecraft.world.phys.Vec3

data class BorderEntityPortalEntryPointEvent(
    val border: WorldBorder,
    override val level: ServerLevel,
    val entity: Entity,
    val pos: Vec3
): CancellableEvent.Typed<BlockPos>(), LevelEvent