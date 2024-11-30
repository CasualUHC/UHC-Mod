package net.casual.championships.minigame.lobby

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.casual.arcade.minigame.template.location.LocationTemplate
import net.casual.arcade.utils.encodedOptionalFieldOf
import net.minecraft.core.Vec3i
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biomes

class CasualLobbyData(
    val position: Vec3i = Vec3i(0, 1, 0),
    val spawn: LocationTemplate = LocationTemplate.DEFAULT,
    val podium: LocationTemplate = LocationTemplate.DEFAULT,
    val podiumView: LocationTemplate = LocationTemplate.DEFAULT,
    val fireworkLocations: List<LocationTemplate> = listOf(),
    val biome: ResourceKey<Biome> = Biomes.THE_VOID,
    val packs: List<String> = listOf()
) {
    companion object {
        val DEFAULT = CasualLobbyData()

        val CODEC: Codec<CasualLobbyData> = RecordCodecBuilder.create { instance ->
            instance.group(
                Vec3i.CODEC.fieldOf("position").forGetter(CasualLobbyData::position),
                LocationTemplate.CODEC.fieldOf("spawn").forGetter(CasualLobbyData::spawn),
                LocationTemplate.CODEC.fieldOf("podium").forGetter(CasualLobbyData::podium),
                LocationTemplate.CODEC.fieldOf("podium_view").forGetter(CasualLobbyData::podiumView),
                LocationTemplate.CODEC.listOf().fieldOf("firework_locations").forGetter(CasualLobbyData::fireworkLocations),
                ResourceKey.codec(Registries.BIOME).encodedOptionalFieldOf("biome", Biomes.THE_VOID).forGetter(CasualLobbyData::biome),
                Codec.STRING.listOf().encodedOptionalFieldOf("packs", listOf()).forGetter(CasualLobbyData::packs)
            ).apply(instance, ::CasualLobbyData)
        }
    }
}