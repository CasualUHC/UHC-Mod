package net.casual.championships.common.event

import net.casual.arcade.events.server.player.PlayerEvent
import net.casual.championships.common.util.AntiCheat
import net.minecraft.server.level.ServerPlayer

data class PlayerCheatEvent(
    override val player: ServerPlayer,
    val type: AntiCheat.Type
): PlayerEvent