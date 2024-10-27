package net.casual.championships.common.items

import com.mojang.serialization.Codec
import eu.pb4.polymer.core.api.item.PolymerItem
import eu.pb4.polymer.core.api.other.PolymerComponent
import net.casual.championships.common.CommonMod
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.TooltipFlag
import xyz.nucleoid.packettweaker.PacketContext

class ForwardFacingPlayerHead(properties: Properties): Item(properties), PolymerItem {
    override fun getPolymerItem(stack: ItemStack, context: PacketContext): Item {
        return Items.PLAYER_HEAD
    }

    override fun getPolymerItemModel(stack: ItemStack, context: PacketContext): ResourceLocation {
        val small = stack.get(IS_SMALL_BRAIN) ?: false
        return if (small) SMALL_MODEL_ID else LARGE_MODEL_ID
    }

    override fun getPolymerItemStack(
        stack: ItemStack,
        flag: TooltipFlag,
        context: PacketContext
    ): ItemStack {
        val copy = super.getPolymerItemStack(stack, flag, context)
        copy.set(DataComponents.PROFILE, stack.get(DataComponents.PROFILE))
        return copy
    }

    companion object {
        private val SMALL_MODEL_ID = CommonMod.id("gui/small_forward_facing_player_head")
        private val LARGE_MODEL_ID = CommonMod.id("gui/large_forward_facing_player_head")

        val IS_SMALL_BRAIN: DataComponentType<Boolean> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            CommonMod.id("small_brain"),
            DataComponentType.builder<Boolean>().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build()
        )

        init {
            PolymerComponent.registerDataComponent(IS_SMALL_BRAIN)
        }
    }
}