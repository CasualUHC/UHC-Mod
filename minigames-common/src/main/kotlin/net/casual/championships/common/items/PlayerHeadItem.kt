package net.casual.championships.common.items

import net.casual.arcade.utils.ItemUtils.isOf
import net.casual.championships.common.util.CommonItems
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile
import kotlin.jvm.optionals.getOrNull

class PlayerHeadItem(properties: Properties): HeadItem(properties) {
    override fun addEffects(player: ServerPlayer) {
        player.addEffect(MobEffectInstance(MobEffects.REGENERATION, 60, 2))
        player.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SPEED, 15 * 20, 1))
        player.addEffect(MobEffectInstance(MobEffects.SATURATION, 5, 4))
    }

    override fun getName(stack: ItemStack): Component? {
        if (stack.isOf(CommonItems.PLAYER_HEAD)) {
            val name = stack.get(DataComponents.PROFILE)?.name?.getOrNull()
            if (name != null) {
                return Component.translatable("${Items.PLAYER_HEAD.descriptionId}.named", name)
            }
        }
        return super.getName(stack)
    }

    companion object {
        fun create(player: ServerPlayer): ItemStack {
            val stack = ItemStack(CommonItems.PLAYER_HEAD)
            stack.set(DataComponents.PROFILE, ResolvableProfile(player.gameProfile))
            return stack
        }
    }
}