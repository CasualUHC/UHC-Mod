package net.casual.championships.common.util

import net.casual.championships.common.CommonMod.id
import net.casual.championships.common.items.*
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties

object CommonItems {
    val MYSTERIOUS_PEARL = register("mysterious_pearl", ::MysteriousPearlItem)

    val GOLDEN_HEAD = register("golden_head", ::GoldenHeadItem)
    val PLAYER_HEAD = register("player_head", ::PlayerHeadItem)

    val FORWARD_FACING_PLAYER_HEAD = register("forward_facing_player_head", ::ForwardFacingPlayerHead)

    val DISPLAY = register("display", ::DisplayItem)
    val TINTED_DISPLAY = register("tinted_display", ::TintedDisplayItem)

    // TODO: this should probably be moved
    val MINESWEEPER = register("minesweeper", ::MinesweeperItem)

    fun noop() {

    }
    
    private fun register(path: String, provider: (Properties) -> Item): Item {
        val key = ResourceKey.create(Registries.ITEM, id(path))
        val properties = Properties().setId(key)
        return Registry.register(BuiltInRegistries.ITEM, key, provider.invoke(properties))
    }
}