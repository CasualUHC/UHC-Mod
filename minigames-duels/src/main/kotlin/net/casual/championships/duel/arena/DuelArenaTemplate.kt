package net.casual.championships.duel.arena

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.casual.arcade.minigame.area.StructureArea
import net.casual.arcade.utils.StructureUtils
import net.casual.championships.common.util.CommonConfig
import net.minecraft.server.level.ServerLevel

class DuelArenaTemplate(val name: String) {
    private val pair by lazy {
        StructureUtils.readWithData(arenas.resolve(this.name), DuelArenaData.CODEC)
    }

    fun create(level: ServerLevel): DuelArena {
        val (structure, data) = this.pair
        return DuelArena(StructureArea(structure, data.position, level), data.teleporter)
    }

    companion object {
        private val arenas = CommonConfig.resolve("arenas")

        val CODEC: Codec<DuelArenaTemplate> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("name").forGetter(DuelArenaTemplate::name)
            ).apply(instance, ::DuelArenaTemplate)
        }
    }
}