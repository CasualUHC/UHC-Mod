package net.casual.championships.uhc

import eu.pb4.mapcanvas.api.core.*
import it.unimi.dsi.fastutil.doubles.Double2ObjectFunction
import it.unimi.dsi.fastutil.doubles.Double2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.casual.arcade.dimensions.level.vanilla.VanillaLikeLevel
import net.casual.arcade.resources.font.heads.PlayerHeadComponents
import net.casual.arcade.utils.ComponentUtils
import net.casual.arcade.utils.ComponentUtils.literal
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.ComponentUtils.yellow
import net.casual.arcade.utils.TeamUtils.color
import net.casual.championships.common.CommonMod
import net.casual.championships.uhc.border.UHCBorderSize
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BiomeTags
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.border.BorderStatus
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class UHCMapRenderer(private val uhc: UHCMinigame) {
    private val canvases = LinkedHashMap<ResourceKey<Level>, CanvasData>()
    private val maps = Object2ObjectOpenHashMap<ResourceKey<Level>, Double2ObjectMap<CanvasImage>>(3)

    fun startWatching(player: ServerPlayer) {
        for (data in this.canvases.values) {
            data.canvas.addPlayer(player)
        }
    }

    fun stopWatching(player: ServerPlayer) {
        for (data in this.canvases.values) {
            data.canvas.removePlayer(player)
        }
    }

    fun getMaps(): List<ItemStack> {
        return this.canvases.values.map { data ->
            val map = data.canvas.asStack()
            val model = data.model
            if (model != null) {
                map.set(DataComponents.ITEM_MODEL, model)
            }
            map
        }
    }

    fun clear() {
        this.maps.clear()
    }

    fun update(level: ServerLevel) {
        val dimension = VanillaLikeLevel.getLikeDimension(level)
        val (canvas, _, _, sizeIcon, playerIcons) = this.canvases.getOrPut(level.dimension()) {
            val (dimensionName, model) = when (dimension) {
                Level.OVERWORLD -> "overworld" to OVERWORLD_ID
                Level.NETHER -> "nether" to NETHER_ID
                Level.END -> "end" to END_ID
                else -> level.dimension().location().path to null
            }

            val canvas = DrawableCanvas.create()
            val formattedDimension = dimensionName.literal().mini().yellow()
            CanvasData(
                canvas,
                model,
                canvas.createIcon(MapDecorationTypes.TARGET_X, true, 26, 220, 0, formattedDimension),
                canvas.createIcon(MapDecorationTypes.TARGET_X, true, 26, 236, 0, null),
                Object2ObjectOpenHashMap()
            )
        }

        var startSize = this.uhc.getCurrentBorderSizeFor(level, UHCBorderSize.Start)
        val endSize = this.uhc.getCurrentBorderSizeFor(level, UHCBorderSize.End)

        val border = level.worldBorder
        if (border.size == endSize) {
            startSize = endSize
        }
        startSize = max(startSize * 1.1, 32.0)

        val (edge, outer) = if (border.status == BorderStatus.STATIONARY) {
            CanvasColor.LAPIS_BLUE_HIGH to CanvasColor.LAPIS_BLUE_NORMAL
        } else {
            CanvasColor.DULL_RED_HIGH to CanvasColor.DULL_RED_NORMAL
        }

        val borderMinX = border.minX
        val borderMinZ = border.minZ
        val borderMaxX = border.maxX
        val borderMaxZ = border.maxZ

        val scale = startSize / 128
        val scaledBorderMinX = Mth.floor(borderMinX / scale + 0.5) + 64
        val scaledBorderMinZ = Mth.floor(borderMinZ / scale + 0.5) + 64
        val scaledBorderMaxX = Mth.floor(borderMaxX / scale - 0.5) + 64
        val scaledBorderMaxZ = Mth.floor(borderMaxZ / scale - 0.5) + 64

        val map = this.getOrCreateMap(level, dimension, border.centerX, border.centerZ, startSize)
        for (x in 0..< 128) {
            for (z in 0..< 128) {
                val isInCenter = x in (scaledBorderMinX)..scaledBorderMaxX &&
                        z in (scaledBorderMinZ..scaledBorderMaxZ)
                if (!isInCenter) {
                    canvas.set(x, z, outer)
                    continue
                }
                if (x == scaledBorderMinX || x == scaledBorderMaxX) {
                    canvas.set(x, z, edge)
                    continue
                }
                if (z == scaledBorderMinZ || z == scaledBorderMaxZ) {
                    canvas.set(x, z, edge)
                    continue
                }
                canvas.setRaw(x, z, map.getRaw(x, z))
            }
        }

        val roundedStartSize = startSize.roundToInt()
        sizeIcon.text = "$roundedStartSize x $roundedStartSize".literal().mini().yellow()

        for (players in level.players()) {
            if (this.isPlayerValidForIcon(players, level, border.centerX, border.centerZ, startSize)) {
                playerIcons.computeIfAbsent(players.uuid) {
                    canvas.createIcon(MapDecorationTypes.TARGET_X, true, 0, 0, 0, null)
                }
            }
        }

        val playerScale = startSize / 256
        val players = this.uhc.server.playerList
        val iter = playerIcons.iterator()
        for ((uuid, icon) in iter) {
            val player = players.getPlayer(uuid)
            if (player == null || !this.isPlayerValidForIcon(player, level, border.centerX, border.centerZ, startSize)) {
                canvas.removeIcon(icon)
                iter.remove()
                continue
            }
            this.updatePlayerIcon(icon, player, playerScale)
        }

        canvas.sendUpdates()
    }

    private fun isPlayerValidForIcon(
        player: ServerPlayer,
        level: ServerLevel,
        centerX: Double,
        centerZ: Double,
        size: Double
    ): Boolean {
        if (player.level().dimension() != level.dimension()) {
            return false
        }
        if (this.uhc.players.isSpectating(player)) {
            return false
        }
        return (abs(player.x - centerX) < size / 2) && (abs(player.z - centerZ) < size / 2)
    }

    private fun updatePlayerIcon(icon: CanvasIcon, player: ServerPlayer, playerScale: Double) {
        val scaledPlayerX = (player.x / playerScale).toInt() + 128
        val scaledPlayerZ = (player.z / playerScale).toInt() + 116

        icon.move(scaledPlayerX, scaledPlayerZ, 0)
        // if (icon.text == null) {
            val head = PlayerHeadComponents.getHead(player).getNow(null)
            if (head != null) {
                icon.text = Component.empty()
                    .append(head)
                    .append(ComponentUtils.space(-10))
                    .append(UHCComponents.Bitmap.PLAYER_BACKGROUND.color(player.team))
                    .append(ComponentUtils.space(-1))
            }
        // }
    }

    private fun getOrCreateMap(
        level: ServerLevel,
        dimension: ResourceKey<Level>,
        centerX: Double,
        centerZ: Double,
        size: Double
    ): CanvasImage {
        val canvases = this.maps.getOrPut(level.dimension()) { Double2ObjectLinkedOpenHashMap() }
        return canvases.computeIfAbsent(size, Double2ObjectFunction {
            val corner = BlockPos.containing(
                centerX - size / 2,
                level.seaLevel.toDouble() + 20,
                centerZ - size / 2
            )
            if (size > 128 || dimension == Level.NETHER) {
                this.createBiomeMap(level, corner, size / 128)
            } else {
                this.createBlockMap(level, corner, size / 128)
            }
        })
    }

    private fun createBlockMap(
        level: ServerLevel,
        from: BlockPos,
        step: Double
    ): CanvasImage {
        val canvas = CanvasImage(128, 128)
        val pos = from.mutable()
        var dx = 0.0
        var dy = 0.0
        for (x in 0..< 128) {
            for (y in 0..< 128) {
                val chunk = level.getChunk(pos)
                var height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE,pos.x and 15, pos.z and 15)
                var state: BlockState
                do {
                    pos.setY(--height)
                    state = chunk.getBlockState(pos)
                } while (state.getMapColor(level, pos) == MapColor.NONE && height > level.minY)

                canvas.set(x, y, state.getMapColor(level, pos), MapColor.Brightness.NORMAL)
                dy += step
                pos.setZ(from.z + Mth.floor(dy))
            }
            dx += step
            dy = 0.0
            pos.setX(from.x + Mth.floor(dx))
            pos.setZ(from.z)
        }
        return canvas
    }

    private fun createBiomeMap(
        level: ServerLevel,
        from: BlockPos,
        step: Double
    ): CanvasImage {
        val canvas = CanvasImage(128, 128)
        val pos = from.mutable()
        var dx = 0.0
        var dy = 0.0
        for (x in 0..< 128) {
            for (y in 0..< 128) {
                canvas.set(x, y, this.biomeToCanvasColor(level.getBiome(pos), pos))
                dy += step
                pos.setZ(from.z + Mth.floor(dy))
            }
            dx += step
            dy = 0.0
            pos.setX(from.x + Mth.floor(dx))
            pos.setZ(from.z)
        }
        return canvas
    }

    private fun biomeToCanvasColor(biome: Holder<Biome>, pos: BlockPos): CanvasColor {
        if (biome.`is`(BiomeTags.IS_OCEAN) || biome.`is`(BiomeTags.IS_DEEP_OCEAN) || biome.`is`(BiomeTags.IS_RIVER)) {
            return CanvasColor.WATER_BLUE_NORMAL
        }
        if (biome.`is`(BiomeTags.IS_BADLANDS)){
            return CanvasColor.TERRACOTTA_ORANGE_HIGH
        }
        if (biome.`is`(Biomes.DESERT) || biome.`is`(BiomeTags.IS_BEACH)) {
            return CanvasColor.PALE_YELLOW_NORMAL
        }
        if (biome.`is`(BiomeTags.IS_MOUNTAIN)){
            return CanvasColor.STONE_GRAY_NORMAL
        }
        if (biome.`is`(BiomeTags.IS_SAVANNA)){
            return CanvasColor.GREEN_NORMAL
        }
        if (biome.`is`(BiomeTags.IS_JUNGLE)) {
            return CanvasColor.EMERALD_GREEN_LOW
        }
        if (biome.`is`(Biomes.SNOWY_PLAINS) || biome.`is`(Biomes.SNOWY_TAIGA)) {
            return CanvasColor.WHITE_NORMAL
        }
        if (biome.`is`(Biomes.NETHER_WASTES)) {
            return CanvasColor.DARK_RED_NORMAL
        }
        if (biome.`is`(Biomes.WARPED_FOREST)) {
            return CanvasColor.BRIGHT_TEAL_NORMAL
        }
        if (biome.`is`(Biomes.CRIMSON_FOREST)) {
            return CanvasColor.RED_NORMAL
        }
        if (biome.`is`(Biomes.BASALT_DELTAS)) {
            return CanvasColor.DEEPSLATE_GRAY_NORMAL
        }
        if (biome.`is`(Biomes.SOUL_SAND_VALLEY)) {
            return CanvasColor.BROWN_NORMAL
        }
        if (biome.`is`(Biomes.END_HIGHLANDS) || biome.`is`(Biomes.END_MIDLANDS) || biome.`is`(Biomes.END_BARRENS)) {
            return CanvasColor.PALE_YELLOW_LOW
        }
        if (biome.`is`(Biomes.THE_VOID) || biome.`is`(Biomes.SMALL_END_ISLANDS)) {
            return CanvasColor.BLACK_NORMAL
        }
        if (biome.`is`(Biomes.THE_END)) {
            if (pos.x * pos.x + pos.z * pos.z < 50 * 50) {
                return CanvasColor.PALE_YELLOW_LOW
            }
            return CanvasColor.BLACK_NORMAL
        }
        return CanvasColor.PALE_GREEN_NORMAL
    }

    private data class CanvasData(
        val canvas: PlayerCanvas,
        val model: ResourceLocation?,
        val dimensionIcon: CanvasIcon,
        val sizeIcon: CanvasIcon,
        val playerIcons: MutableMap<UUID, CanvasIcon>
    )

    companion object {
        private val OVERWORLD_ID = CommonMod.id("gui/overworld_map")
        private val NETHER_ID = CommonMod.id("gui/nether_map")
        private val END_ID = CommonMod.id("gui/end_map")

        fun noop() {

        }
    }
}