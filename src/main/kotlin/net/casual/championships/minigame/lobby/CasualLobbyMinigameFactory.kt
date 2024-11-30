package net.casual.championships.minigame.lobby

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.casual.arcade.dimensions.level.CustomLevel
import net.casual.arcade.dimensions.level.LevelPersistence
import net.casual.arcade.dimensions.level.builder.CustomLevelBuilder
import net.casual.arcade.dimensions.utils.impl.VoidChunkGenerator
import net.casual.arcade.minigame.area.PlaceableArea
import net.casual.arcade.minigame.area.StructureArea
import net.casual.arcade.minigame.serialization.MinigameCreationContext
import net.casual.arcade.minigame.serialization.MinigameFactory
import net.casual.arcade.minigame.template.area.PlaceableAreaTemplate
import net.casual.arcade.minigame.utils.MinigameResources
import net.casual.arcade.resources.pack.PackInfo
import net.casual.arcade.resources.utils.ResourcePackUtils.toPackInfo
import net.casual.arcade.utils.StructureUtils
import net.casual.arcade.utils.codec.CodecProvider
import net.casual.arcade.utils.encodedOptionalFieldOf
import net.casual.championships.CasualMod
import net.casual.championships.common.util.CommonConfig
import net.casual.championships.duel.arena.DuelArenasTemplate
import net.casual.championships.minigame.CasualMinigames
import net.casual.championships.resources.CasualResourcePackHost
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.dimension.BuiltinDimensionTypes
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
import java.util.*

class CasualLobbyMinigameFactory(
    private val name: Optional<String>,
    private val duelArenas: List<DuelArenasTemplate>
): MinigameFactory {
    private lateinit var structure: StructureTemplate
    private var data = CasualLobbyData.DEFAULT

    override fun codec(): MapCodec<out MinigameFactory> {
        return CODEC
    }

    override fun create(context: MinigameCreationContext): CasualLobbyMinigame {
        val level = CustomLevelBuilder.build(context.server) {
            randomDimensionKey()
            dimensionType(BuiltinDimensionTypes.OVERWORLD)
            chunkGenerator(VoidChunkGenerator(context.server, data.biome))
            defaultLevelProperties()
            tickTime(true)
            persistence(LevelPersistence.Temporary)
        }
        val area = this.createPlaceableArea(level)
        val minigame = CasualLobbyMinigame(
            context.server,
            context.uuid,
            area,
            this.data.spawn.get(level),
            this.data.podium,
            this.data.podiumView,
            this.data.fireworkLocations,
            this.duelArenas,
            this
        )
        CasualMinigames.setCasualUI(minigame)
        // TODO:
        minigame.resources.add(object: MinigameResources {
            override fun getPacks(): Collection<PackInfo> {
                @Suppress("DEPRECATION")
                return data.packs.mapNotNull { pack ->
                    val hosted = CasualResourcePackHost.getHostedPack(pack)
                    if (hosted != null) {
                        hosted.toPackInfo(!CasualMod.config.dev)
                    } else {
                        CasualMod.logger.error("Failed to load lobby pack $pack")
                        null
                    }
                }
            }
        })
        return minigame
    }

    private fun createPlaceableArea(level: CustomLevel): PlaceableArea {
        if (this.name.isEmpty) {
            return PlaceableAreaTemplate.DEFAULT.create(level)
        }
        if (this::structure.isInitialized) {
            return StructureArea(this.structure, this.data.position, level)
        }
        val path = lobbies.resolve(this.name.get())
        try {
            val (structure, data) = StructureUtils.readWithData(path, CasualLobbyData.CODEC)
            this.data = data
            this.structure = structure
            return StructureArea(structure, data.position, level)
        } catch (e: Exception) {
            CasualMod.logger.error("Failed to read structure: $path", e)
            return PlaceableAreaTemplate.DEFAULT.create(level)
        }
    }

    companion object: CodecProvider<CasualLobbyMinigameFactory> {
        private val lobbies = CommonConfig.resolve("lobbies")

        override val ID: ResourceLocation = CasualLobbyMinigame.ID

        override val CODEC: MapCodec<out CasualLobbyMinigameFactory> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.STRING.optionalFieldOf("lobby").forGetter(CasualLobbyMinigameFactory::name),
                DuelArenasTemplate.CODEC.listOf().encodedOptionalFieldOf("duel_arenas", listOf()).forGetter(CasualLobbyMinigameFactory::duelArenas)
            ).apply(instance, ::CasualLobbyMinigameFactory)
        }
    }
}