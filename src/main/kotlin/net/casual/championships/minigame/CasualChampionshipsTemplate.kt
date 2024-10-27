package net.casual.championships.minigame

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.casual.arcade.dimensions.level.builder.CustomLevelBuilder
import net.casual.arcade.dimensions.utils.impl.VoidChunkGenerator
import net.casual.arcade.minigame.lobby.LobbyMinigame
import net.casual.arcade.minigame.template.lobby.LobbyTemplate
import net.casual.arcade.minigame.template.minigame.MinigameData
import net.casual.arcade.minigame.template.minigame.SimpleMinigamesTemplate
import net.casual.arcade.minigame.utils.MinigameResources
import net.casual.arcade.resources.pack.PackInfo
import net.casual.arcade.resources.utils.ResourcePackUtils.toPackInfo
import net.casual.arcade.utils.codec.CodecProvider
import net.casual.arcade.utils.encodedOptionalFieldOf
import net.casual.championships.CasualMod
import net.casual.championships.resources.CasualResourcePackHost
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.dimension.BuiltinDimensionTypes

class CasualChampionshipsTemplate(
    name: String = "default",
    lobby: LobbyTemplate = LobbyTemplate.DEFAULT,
    operators: List<String> = listOf(),
    minigames: List<MinigameData>,
    repeat: Boolean = true,
    private val lobbyBiome: ResourceKey<Biome>,
    private val additionalPacks: List<String>,
    private val lobbyPacks: List<String>
): SimpleMinigamesTemplate(name, lobby, operators, minigames, repeat) {
    override fun createLobby(server: MinecraftServer): LobbyMinigame {
        val minigame = super.createLobby(server)
        minigame.addResources(object: MinigameResources {
            override fun getPacks(): Collection<PackInfo> {
                @Suppress("DEPRECATION")
                return lobbyPacks.mapNotNull { pack ->
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

    override fun getAdditionalPacks(): Iterable<PackInfo> {
        val packs = ArrayList<PackInfo>()
        for (pack in this.additionalPacks) {
            // TODO:
            @Suppress("DEPRECATION")
            val hosted = CasualResourcePackHost.getHostedPack(pack)
            if (hosted == null) {
                CasualMod.logger.error("Failed to load additional pack $pack")
                continue
            }
            packs.add(hosted.toPackInfo(!CasualMod.config.dev))
        }

        return CasualResourcePackHost.getCommonPacks().mapTo(packs) {
            it.toPackInfo(!CasualMod.config.dev)
        }
    }

    override fun getLobbyLevel(server: MinecraftServer): ServerLevel {
        return CustomLevelBuilder.build(server) {
            randomDimensionKey()
            defaultLevelProperties()
            dimensionType(BuiltinDimensionTypes.OVERWORLD)
            chunkGenerator(VoidChunkGenerator(server, lobbyBiome))
            tickTime(true)
        }
    }

    override fun codec(): MapCodec<out CasualChampionshipsTemplate> {
        return CODEC
    }

    companion object: CodecProvider<CasualChampionshipsTemplate> {
        override val ID: ResourceLocation = CasualMod.id("championships")

        override val CODEC: MapCodec<CasualChampionshipsTemplate> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.STRING.encodedOptionalFieldOf("name", "default").forGetter(SimpleMinigamesTemplate::name),
                LobbyTemplate.CODEC.fieldOf("lobby").forGetter(SimpleMinigamesTemplate::lobby),
                Codec.STRING.listOf().fieldOf("operators").forGetter(SimpleMinigamesTemplate::operators),
                MinigameData.CODEC.listOf().fieldOf("minigames").forGetter(SimpleMinigamesTemplate::minigames),
                Codec.BOOL.fieldOf("repeat").forGetter(SimpleMinigamesTemplate::repeat),
                ResourceKey.codec(Registries.BIOME).encodedOptionalFieldOf("lobby_biome", Biomes.PLAINS)
                    .forGetter(CasualChampionshipsTemplate::lobbyBiome),
                Codec.STRING.listOf().encodedOptionalFieldOf("additional_packs", listOf())
                    .forGetter(CasualChampionshipsTemplate::additionalPacks),
                Codec.STRING.listOf().encodedOptionalFieldOf("lobby_packs", listOf())
                    .forGetter(CasualChampionshipsTemplate::lobbyPacks)
            ).apply(instance, ::CasualChampionshipsTemplate)
        }
    }
}