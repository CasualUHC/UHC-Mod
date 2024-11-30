package net.casual.championships.duel.arena

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.casual.arcade.minigame.template.teleporter.EntityTeleporter
import net.minecraft.core.Vec3i

class DuelArenaData(
    val position: Vec3i,
    val teleporter: EntityTeleporter
) {
    companion object {
        val CODEC: Codec<DuelArenaData> = RecordCodecBuilder.create { instance ->
            instance.group(
                Vec3i.CODEC.fieldOf("position").forGetter(DuelArenaData::position),
                EntityTeleporter.CODEC.fieldOf("teleporter").forGetter(DuelArenaData::teleporter)
            ).apply(instance, ::DuelArenaData)
        }
    }
}