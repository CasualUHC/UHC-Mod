package net.casual.championships.common.items

import eu.pb4.polymer.core.api.item.PolymerItem
import net.casual.championships.common.util.CommonComponents
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.*
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import xyz.nucleoid.packettweaker.PacketContext

abstract class HeadItem(properties: Properties): BlockItem(Blocks.PLAYER_HEAD, properties), PolymerItem {
    abstract fun addEffects(player: ServerPlayer)

    open fun getResolvableProfile(stack: ItemStack): ResolvableProfile? {
        return stack.get(DataComponents.PROFILE)
    }

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResult {
        if (player is ServerPlayer) {
            this.addEffects(player)
            player.swing(usedHand, true)
            val stack = player.getItemInHand(usedHand)
            player.cooldowns.addCooldown(stack, 20)
            stack.consume(1, player)
            return InteractionResult.CONSUME.heldItemTransformedTo(stack)
        }
        return super.use(level, player, usedHand)
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player
        if (player is ServerPlayer) {
            this.addEffects(player)
            player.swing(context.hand, true)
            player.cooldowns.addCooldown(context.itemInHand, 20)
            if (!player.abilities.instabuild) {
                context.itemInHand.shrink(1)
            }
            return InteractionResult.CONSUME
        }
        return super.useOn(context)
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        tooltipComponents.add(CommonComponents.HEAD_TOOLTIP.withStyle(ChatFormatting.GRAY))
    }

    override fun getPolymerItem(itemStack: ItemStack, context: PacketContext): Item {
        return Items.PLAYER_HEAD
    }

    override fun getPolymerItemStack(
        stack: ItemStack,
        tooltip: TooltipFlag,
        context: PacketContext
    ): ItemStack {
        val out = super.getPolymerItemStack(stack, tooltip, context)
        val profile = this.getResolvableProfile(stack)
        if (profile != null) {
            out.set(DataComponents.PROFILE, profile)
        }
        return out
    }
}

