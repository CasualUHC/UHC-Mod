package net.casual.championships.common.items

import eu.pb4.polymer.core.api.item.PolymerItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import xyz.nucleoid.packettweaker.PacketContext

class DisplayItem(properties: Properties): Item(properties), PolymerItem {
    override fun getPolymerItem(stack: ItemStack, context: PacketContext): Item {
        return Items.POPPED_CHORUS_FRUIT
    }
}