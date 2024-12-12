package net.casual.championships.uhc

import com.mojang.serialization.MapCodec
import net.casual.arcade.dimensions.level.vanilla.VanillaLikeLevel
import net.casual.arcade.minigame.template.teleporter.EntityTeleporter
import net.casual.arcade.minigame.template.teleporter.ShapedTeleporter
import net.casual.arcade.utils.StructureUtils
import net.casual.arcade.utils.impl.Location
import net.casual.arcade.utils.isOceanOrRiver
import net.casual.arcade.utils.isOf
import net.casual.arcade.visuals.shapes.LevelSurfaceShape
import net.casual.arcade.visuals.shapes.Regular2DPolygonShape
import net.casual.arcade.visuals.shapes.ShapePoints
import net.minecraft.core.BlockPos
import net.minecraft.core.BlockPos.MutableBlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BiomeTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.SpawnPlacementTypes
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
import net.minecraft.world.phys.Vec3
import net.minecraft.world.scores.PlayerTeam
import kotlin.math.max

object UHCSpreadTeleporter: ShapedTeleporter() {
    private val structurePath = UHCMod.container.findPath("structures").get()
    private val overworldSpawn by lazy { StructureUtils.read(structurePath.resolve("overworld_spawn.nbt")) }
    private val netherSpawn by lazy { StructureUtils.read(structurePath.resolve("nether_spawn.nbt")) }
    private val endSpawn by lazy { StructureUtils.read(structurePath.resolve("end_spawn.nbt")) }

    override fun teleportTeam(team: PlayerTeam, entities: Collection<Entity>, location: Location) {
        val (level, pos, rot) = location
        val origin = BlockPos.containing(pos)
        val blockPositions = BlockPos.spiralAround(origin, 16, Direction.EAST, Direction.SOUTH)
        for (blockPos in blockPositions) {
            val adjusted = this.getTopNonCollidingPos(level, blockPos) ?: continue
            super.teleportTeam(team, entities, Location.of(adjusted.center, rot, level))
            return
        }

        val spawn = when (VanillaLikeLevel.getLikeDimension(level)) {
            Level.NETHER -> this.netherSpawn
            Level.END -> this.endSpawn
            else -> this.overworldSpawn
        }
        val corner = origin.offset(-spawn.size.x / 2, -spawn.size.y / 2 - 1, -spawn.size.z / 2)
        val settings = StructurePlaceSettings().setKnownShape(true)
            .setLiquidSettings(LiquidSettings.APPLY_WATERLOGGING)

        spawn.placeInWorld(level, corner, corner, settings, level.random, Block.UPDATE_CLIENTS)

        super.teleportTeam(team, entities, Location.of(origin.center, rot, level))
    }

    override fun createShape(level: ServerLevel, points: Int): ShapePoints {
        val border = level.worldBorder
        val center = Vec3(border.centerX, level.seaLevel + 1.0, border.centerZ)
        val polygon = Regular2DPolygonShape.createHorizontal(center, border.size * 0.45, points)
        return LevelSurfaceShape(level) { steps ->
            BiomeRounderIterator(level, polygon.iterator(steps), polygon.sideLength * 0.3)
        }
    }

    override fun codec(): MapCodec<out EntityTeleporter> {
        return MapCodec.unit(this)
    }

    private fun getTopNonCollidingPos(level: ServerLevel, initial: BlockPos): BlockPos? {
        val chunk = level.getChunk(initial)
        val y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, initial.x, initial.z)
        val pos = MutableBlockPos(initial.x, y, initial.z)
        if (level.dimensionType().hasCeiling()) {
            do {
                pos.move(Direction.DOWN)
            } while (!chunk.getBlockState(pos).isAir)

            do {
                pos.move(Direction.DOWN)
            } while (chunk.getBlockState(pos).isAir && pos.y > level.minY)
        }

        val adjusted = SpawnPlacementTypes.ON_GROUND.adjustSpawnPosition(level, pos.immutable())
        if (SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(level, adjusted, EntityType.PLAYER)) {
            return adjusted
        }
        return null
    }

    private class BiomeRounderIterator(
        private val level: ServerLevel,
        private val wrapped: Iterator<Vec3>,
        private val delta: Double
    ): Iterator<Vec3> {
        override fun hasNext(): Boolean {
            return this.wrapped.hasNext()
        }

        override fun next(): Vec3 {
            val point = this.wrapped.next()
            val containing = BlockPos.containing(point)
            val biome = this.level.getBiome(containing)
            if (!biome.isOceanOrRiver()) {
                return point
            }
            val pair = this.level.findClosestBiome3d(
                { holder -> !holder.isOceanOrRiver() && !holder.isOf(BiomeTags.IS_BEACH) },
                containing, this.delta.toInt(), max((this.delta / 10).toInt(), 32), this.level.height
            )
            if (pair == null || !this.level.isInWorldBounds(pair.first)) {
                // We have no other option...
                return point
            }
            return pair.first.center
        }
    }
}