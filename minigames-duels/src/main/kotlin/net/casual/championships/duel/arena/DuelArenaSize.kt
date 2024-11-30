package net.casual.championships.duel.arena

import net.casual.arcade.utils.ItemUtils.named
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

enum class DuelArenaSize(val display: ItemStack) {
    Small(Items.SMALL_AMETHYST_BUD.named(Component.translatable("casual.duel.size.small"))),
    Medium(Items.MEDIUM_AMETHYST_BUD.named(Component.translatable("casual.duel.size.medium"))),
    Large(Items.LARGE_AMETHYST_BUD.named(Component.translatable("casual.duel.size.large")))
}