package net.casual.championships.uhc

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.casual.arcade.dimensions.level.LevelPersistence
import net.casual.arcade.dimensions.level.vanilla.VanillaDimension
import net.casual.arcade.dimensions.level.vanilla.VanillaLikeLevelsBuilder
import net.casual.arcade.minigame.serialization.MinigameCreationContext
import net.casual.arcade.minigame.serialization.MinigameFactory
import net.casual.arcade.utils.ResourceUtils
import net.casual.arcade.utils.codec.CodecProvider
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.StringRepresentable
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.WorldOptions
import java.util.*

data class DimensionWithSeed(
    val key: Optional<ResourceKey<Level>>,
    val seed: Optional<Long>
) {
    companion object {
        val CODEC: Codec<DimensionWithSeed> = RecordCodecBuilder.create { instance ->
            instance.group(
                ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(DimensionWithSeed::key),
                Codec.LONG.optionalFieldOf("seed").forGetter(DimensionWithSeed::seed)
            ).apply(instance, ::DimensionWithSeed)
        }
    }
}

open class UHCMinigameFactory(
    private val dimensions: Map<VanillaDimension, DimensionWithSeed>
): MinigameFactory {
    override fun codec(): MapCodec<out MinigameFactory> {
        return CODEC
    }

    override fun create(context: MinigameCreationContext): UHCMinigame {
        if (this.dimensions.size != 3) {
            throw IllegalArgumentException("UHCMinigameFactory must provide all 3 vanilla dimensions")
        }

        val seed = WorldOptions.randomSeed()
        val levels = VanillaLikeLevelsBuilder.build(context.server) {
            for ((dimension, pair) in dimensions) {
                set(dimension) {
                    dimensionKey(pair.key.orElseGet { randomDimensionKey(dimension.serializedName) })
                    seed(pair.seed.orElse(seed))
                    defaultLevelProperties()
                    persistence(LevelPersistence.Permanent)
                }
            }
        }

        return UHCMinigame(
            context.server,
            context.uuid,
            levels.getOrThrow(VanillaDimension.Overworld),
            levels.getOrThrow(VanillaDimension.Nether),
            levels.getOrThrow(VanillaDimension.End),
            this
        )
    }

    private fun randomDimensionKey(dimension: String): ResourceKey<Level> {
        return ResourceKey.create(Registries.DIMENSION, ResourceUtils.random { "${dimension}_$it" })
    }

    companion object: CodecProvider<UHCMinigameFactory> {
        override val ID: ResourceLocation
            get() = UHCMinigame.ID

        override val CODEC: MapCodec<out UHCMinigameFactory> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.simpleMap(
                    VanillaDimension.CODEC,
                    DimensionWithSeed.CODEC,
                    StringRepresentable.keys(VanillaDimension.entries.toTypedArray())
                ).fieldOf("dimensions").forGetter(UHCMinigameFactory::dimensions)
            ).apply(instance, ::UHCMinigameFactory)
        }
    }
}