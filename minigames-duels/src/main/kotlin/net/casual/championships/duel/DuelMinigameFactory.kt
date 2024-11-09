package net.casual.championships.duel

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.casual.arcade.minigame.serialization.MinigameCreationContext
import net.casual.arcade.minigame.serialization.MinigameFactory
import net.casual.arcade.utils.codec.CodecProvider
import net.casual.championships.duel.arena.DuelArenasTemplate
import net.minecraft.resources.ResourceLocation

class DuelMinigameFactory(private val settings: DuelSettings): MinigameFactory {
    override fun codec(): MapCodec<out MinigameFactory> {
        return CODEC
    }

    override fun create(context: MinigameCreationContext): DuelMinigame {
        return DuelMinigame(context.server, context.uuid, this.settings)
    }

    companion object: CodecProvider<DuelMinigameFactory> {
        override val ID: ResourceLocation
            get() = DuelMinigame.ID

        override val CODEC: MapCodec<out DuelMinigameFactory> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                DuelArenasTemplate.CODEC.listOf().xmap(::DuelSettings, DuelSettings::arenas).fieldOf("arenas").forGetter(DuelMinigameFactory::settings)
            ).apply(instance, ::DuelMinigameFactory)
        }
    }
}