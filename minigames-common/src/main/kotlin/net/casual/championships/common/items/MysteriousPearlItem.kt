package net.casual.championships.common.items

import eu.pb4.polymer.core.api.item.PolymerItem
import net.casual.championships.common.CommonMod
import net.casual.championships.common.entities.MysteriousPearl
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import xyz.nucleoid.packettweaker.PacketContext

class MysteriousPearlItem(properties: Properties): Item(properties), PolymerItem {
    override fun getPolymerItem(stack: ItemStack, context: PacketContext): Item {
        return Items.POPPED_CHORUS_FRUIT
    }

    override fun getPolymerItemModel(stack: ItemStack, context: PacketContext): ResourceLocation {
        return CommonMod.id("test/mysterious_pearl")
    }

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(usedHand)

        val throwable = MysteriousPearl(level, player)
        throwable.shootFromRotation(player, player.xRot, player.yRot, 0.0F, 1.5F, 1.0F)
        level.addFreshEntity(throwable)

        stack.consume(1, player)

        return InteractionResult.SUCCESS.heldItemTransformedTo(stack)
    }
}