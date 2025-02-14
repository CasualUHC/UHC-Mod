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
import java.util.*

class CasualLobbyData(
    val position: Vec3i = Vec3i(0, 1, 0),
    val spawn: LocationTemplate = LocationTemplate.DEFAULT,
    val podium: LocationTemplate = LocationTemplate.DEFAULT,
    val podiumView: LocationTemplate = LocationTemplate.DEFAULT,
    val fireworkLocations: List<LocationTemplate> = listOf(),
    val fireworkColors: List<Int> = DEFAULT_COLORS,
    val biome: ResourceKey<Biome> = Biomes.THE_VOID,
    val raining: Boolean = false,
    val timeOfDay: Optional<Int> = Optional.empty(),
    val packs: List<String> = listOf()
) {
    companion object {
        private val DEFAULT_COLORS = listOf(0x9820e0, 0xbe51e9, 0xd564fb, 0xe07b20, 0xe8a751, 0xfbbe64)

        val DEFAULT = CasualLobbyData()

        val CODEC: Codec<CasualLobbyData> = RecordCodecBuilder.create { instance ->
            instance.group(
                Vec3i.CODEC.fieldOf("position").forGetter(CasualLobbyData::position),
                LocationTemplate.CODEC.fieldOf("spawn").forGetter(CasualLobbyData::spawn),
                LocationTemplate.CODEC.fieldOf("podium").forGetter(CasualLobbyData::podium),
                LocationTemplate.CODEC.fieldOf("podium_view").forGetter(CasualLobbyData::podiumView),
                LocationTemplate.CODEC.listOf().fieldOf("firework_locations").forGetter(CasualLobbyData::fireworkLocations),
                Codec.INT.listOf().encodedOptionalFieldOf("firework_colors", DEFAULT_COLORS).forGetter(CasualLobbyData::fireworkColors),
                ResourceKey.codec(Registries.BIOME).encodedOptionalFieldOf("biome", Biomes.THE_VOID).forGetter(CasualLobbyData::biome),
                Codec.BOOL.optionalFieldOf("raining", false).forGetter(CasualLobbyData::raining),
                Codec.INT.optionalFieldOf("time_of_day").forGetter(CasualLobbyData::timeOfDay),
                Codec.STRING.listOf().encodedOptionalFieldOf("packs", listOf()).forGetter(CasualLobbyData::packs)
            ).apply(instance, ::CasualLobbyData)
        }
    }
}