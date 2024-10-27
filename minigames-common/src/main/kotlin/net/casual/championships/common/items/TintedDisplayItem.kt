package net.casual.championships.common.items

import eu.pb4.polymer.core.api.item.PolymerItem
import net.minecraft.core.component.DataComponents.DYED_COLOR
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.DyedItemColor
import xyz.nucleoid.packettweaker.PacketContext

class TintedDisplayItem(properties: Properties): Item(properties), PolymerItem {
    override fun getPolymerItem(stack: ItemStack, context: PacketContext): Item {
        return Items.WOLF_ARMOR
    }

    override fun getPolymerItemStack(
        stack: ItemStack,
        flag: TooltipFlag,
        context: PacketContext
    ): ItemStack {
        val copy = super.getPolymerItemStack(stack, flag, context)
        copy.set(DYED_COLOR, DyedItemColor(DyedItemColor.getOrDefault(stack, 0xFFFFFF), false))
        return copy
    }
}