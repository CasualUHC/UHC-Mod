package net.casual.championships.missilewars.items

import net.casual.arcade.items.ItemStackFactory
import net.casual.championships.common.CommonMod.id
import net.casual.championships.missilewars.MissileWarsMod
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties

object MissileWarsItems {
    private val FLYING_MACHINE = ItemStackFactory.modeller(register("flying_machine", ::FlyingMachineItem))

    val BOUNCER by FLYING_MACHINE.modelled(MissileWarsMod.id("missile_wars/bouncer"))

    fun noop() {

    }

    private fun register(path: String, provider: (Properties) -> Item): Item {
        val key = ResourceKey.create(Registries.ITEM, id(path))
        val properties = Properties().setId(key)
        return Registry.register(BuiltInRegistries.ITEM, key, provider.invoke(properties))
    }
}