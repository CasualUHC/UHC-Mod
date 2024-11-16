package net.casual.championships.minigame.lobby

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.casual.arcade.dimensions.utils.addCustomLevel
import net.casual.arcade.dimensions.utils.impl.VoidChunkGenerator
import net.casual.arcade.minigame.serialization.MinigameCreationContext
import net.casual.arcade.minigame.serialization.MinigameFactory
import net.casual.arcade.minigame.template.area.PlaceableAreaTemplate
import net.casual.arcade.minigame.template.location.LocationTemplate
import net.casual.arcade.minigame.utils.MinigameResources
import net.casual.arcade.resources.pack.PackInfo
import net.casual.arcade.resources.utils.ResourcePackUtils.toPackInfo
import net.casual.arcade.utils.codec.CodecProvider
import net.casual.arcade.utils.encodedOptionalFieldOf
import net.casual.championships.CasualMod
import net.casual.championships.duel.arena.DuelArenasTemplate
import net.casual.championships.minigame.CasualMinigames
import net.casual.championships.resources.CasualResourcePackHost
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.dimension.BuiltinDimensionTypes

class CasualLobbyMinigameFactory(
    private val area: PlaceableAreaTemplate,
    private val spawn: LocationTemplate,
    private val podium: LocationTemplate,
    private val podiumView: LocationTemplate,
    private val fireworkLocations: List<LocationTemplate>,
    private val duelArenas: List<DuelArenasTemplate>,
    private val biome: ResourceKey<Biome>,
    private val packs: List<String>
): MinigameFactory {
    override fun codec(): MapCodec<out MinigameFactory> {
        return CODEC
    }

    override fun create(context: MinigameCreationContext): CasualLobbyMinigame {
        val level = context.server.addCustomLevel {
            randomDimensionKey()
            dimensionType(BuiltinDimensionTypes.OVERWORLD)
            chunkGenerator(VoidChunkGenerator(context.server, biome))
            defaultLevelProperties()
            tickTime(true)
        }
        val minigame = CasualLobbyMinigame(
            context.server,
            context.uuid,
            this.area.create(level),
            this.spawn.get(level),
            this.podium,
            this.podiumView,
            this.fireworkLocations,
            this.duelArenas
        )
        CasualMinigames.setCasualUI(minigame)
        // TODO:
        minigame.resources.add(object: MinigameResources {
            override fun getPacks(): Collection<PackInfo> {
                @Suppress("DEPRECATION")
                return packs.mapNotNull { pack ->
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

    companion object: CodecProvider<CasualLobbyMinigameFactory> {
        override val ID: ResourceLocation = CasualLobbyMinigame.ID

        override val CODEC: MapCodec<out CasualLobbyMinigameFactory> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                PlaceableAreaTemplate.CODEC.fieldOf("area").forGetter(CasualLobbyMinigameFactory::area),
                LocationTemplate.CODEC.fieldOf("spawn").forGetter(CasualLobbyMinigameFactory::spawn),
                LocationTemplate.CODEC.fieldOf("podium").forGetter(CasualLobbyMinigameFactory::podium),
                LocationTemplate.CODEC.fieldOf("podium_view").forGetter(CasualLobbyMinigameFactory::podiumView),
                LocationTemplate.CODEC.listOf().fieldOf("firework_locations").forGetter(CasualLobbyMinigameFactory::fireworkLocations),
                DuelArenasTemplate.CODEC.listOf().encodedOptionalFieldOf("duel_arenas", listOf()).forGetter(CasualLobbyMinigameFactory::duelArenas),
                ResourceKey.codec(Registries.BIOME).encodedOptionalFieldOf("biome", Biomes.THE_VOID).forGetter(CasualLobbyMinigameFactory::biome),
                Codec.STRING.listOf().encodedOptionalFieldOf("packs", listOf()).forGetter(CasualLobbyMinigameFactory::packs)
            ).apply(instance, ::CasualLobbyMinigameFactory)
        }
    }
}