package net.casual.championships.common.event

import net.casual.arcade.events.server.player.PlayerEvent
import net.minecraft.server.level.ServerPlayer
import kotlin.time.Duration

data class MinesweeperWonEvent(
    override val player: ServerPlayer,
    val time: Duration
): PlayerEvent