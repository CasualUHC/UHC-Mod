package net.casual.championships.common.util

import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.ListenerRegistry.Companion.register
import net.casual.arcade.events.server.level.LevelBlockChangedEvent
import net.casual.arcade.events.server.level.LevelTickEvent
import net.casual.arcade.events.server.player.PlayerBlockPlacedEvent
import net.casual.arcade.extensions.event.LevelExtensionEvent
import net.casual.arcade.utils.PlayerUtils.broadcastToOps
import net.casual.championships.common.event.PlayerCheatEvent
import net.casual.championships.common.extensions.WorldBlockTrackerExtension
import net.casual.championships.common.extensions.WorldBlockTrackerExtension.Companion.blockTracker
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BlockTags
import net.minecraft.world.item.PlaceOnWaterBlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext

object AntiCheat {
    enum class Type {
        FlexibleBlockPlacement
    }

    internal fun registerEvents() {
        GlobalEventHandler.Server.register<LevelExtensionEvent> { it.addExtension(WorldBlockTrackerExtension()) }
        GlobalEventHandler.Server.register<LevelTickEvent> { it.level.blockTracker.tick() }
        GlobalEventHandler.Server.register<LevelBlockChangedEvent> { onBlockChanged(it) }
        GlobalEventHandler.Server.register<PlayerBlockPlacedEvent> { onPlayerBlockPlaced(it) }
    }

    private fun onBlockChanged(event: LevelBlockChangedEvent) {
        if (event.old != event.new)  {
            event.level.blockTracker.track(event.pos, event.old)
        }
    }

    private fun onPlayerBlockPlaced(event: PlayerBlockPlacedEvent) {
        if (detectFlexibleBlockPlacement(event.player, event.context)) {
            GlobalEventHandler.Server.broadcast(PlayerCheatEvent(event.player, Type.FlexibleBlockPlacement))

            val message = Component.literal("Player ").append(event.player.displayName!!).append(" used fbp")
            event.player.server.playerList.players.broadcastToOps(message)

            event.cancel()
        }
    }

    private fun detectFlexibleBlockPlacement(player: ServerPlayer, context: BlockPlaceContext): Boolean {
        if (player.hasPermissions(4)) {
            return false
        }

        val pos = context.clickedPos
        val level = context.level as ServerLevel

        val state = level.getBlockState(pos)

        var couldBePlacedOffset = isSolidForPlacement(context, state)
        var couldBePlacedHere = !couldBePlacedOffset
        for (olderState in level.blockTracker.getTrackedStatesInLastNTicks(pos, 20)) {
            val isSolid = isSolidForPlacement(context, olderState)
            couldBePlacedOffset = couldBePlacedOffset || isSolid
            couldBePlacedHere = couldBePlacedHere || !isSolid

            if (couldBePlacedOffset && couldBePlacedHere) {
                break
            }
        }
        val possible = ArrayList<BlockPos>(2)

        if (couldBePlacedHere) {
            possible.add(pos)
            // Check for placement inside a block
            val shapeContext = CollisionContext.of(player)
            if (!state.getShape(level, pos, shapeContext).isEmpty) {
                return false
            }
            for (olderState in level.blockTracker.getTrackedStatesInLastNTicks(pos, 20)) {
                if (!olderState.getShape(level, pos, shapeContext).isEmpty) {
                    return false
                }
            }
        }

        if (couldBePlacedOffset) {
            possible.add(pos.relative(context.clickedFace))
        }

        for (placedPos in possible) {
            for (direction in Direction.entries) {
                val neighbor = level.getBlockState(placedPos.relative(direction))
                if (isSolidForPlacement(context, neighbor)) {
                    return false
                }
                for (olderNeighbor in level.blockTracker.getTrackedStatesInLastNTicks(placedPos.relative(direction), 20)) {
                    if (isSolidForPlacement(context, olderNeighbor)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun isSolidForPlacement(context: BlockPlaceContext, state: BlockState): Boolean {
        if (state.canBeReplaced(context) && !state.`is`(BlockTags.SLABS)) {
            val fluidsSolid = context.itemInHand.item is PlaceOnWaterBlockItem
            return fluidsSolid && state.fluidState.isSource
        }
        return true
    }
}