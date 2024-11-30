package net.casual.championships.duel.arena

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.item.ItemStack

class DuelArenasTemplate(
    val name: String,
    val display: ItemStack,
    private val small: DuelArenaTemplate,
    private val medium: DuelArenaTemplate,
    private val large: DuelArenaTemplate
) {
    fun getArenaTemplateFor(size: DuelArenaSize): DuelArenaTemplate {
        return when (size) {
            DuelArenaSize.Small -> this.small
            DuelArenaSize.Medium -> this.medium
            DuelArenaSize.Large -> this.large
        }
    }

    companion object {
        val CODEC: Codec<DuelArenasTemplate> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("name").forGetter(DuelArenasTemplate::name),
                ItemStack.SINGLE_ITEM_CODEC.fieldOf("display").forGetter(DuelArenasTemplate::display),
                DuelArenaTemplate.CODEC.fieldOf("small").forGetter(DuelArenasTemplate::small),
                DuelArenaTemplate.CODEC.fieldOf("medium").forGetter(DuelArenasTemplate::medium),
                DuelArenaTemplate.CODEC.fieldOf("large").forGetter(DuelArenasTemplate::large)
            ).apply(instance, ::DuelArenasTemplate)
        }
    }
}