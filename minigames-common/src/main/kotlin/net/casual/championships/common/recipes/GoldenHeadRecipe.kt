package net.casual.championships.common.recipes

import net.casual.arcade.utils.recipe.CraftingRecipeBuilder
import net.casual.championships.common.CommonMod
import net.casual.championships.common.util.CommonItems
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingBookCategory

object GoldenHeadRecipe {
    val INSTANCE = CraftingRecipeBuilder.shaped {
        key(CommonMod.id("golden_head_recipe"))
        category = CraftingBookCategory.MISC
        height = 3
        width = 3
        result = ItemStack(CommonItems.GOLDEN_HEAD)
        val x = Items.GOLD_INGOT
        val o = CommonItems.PLAYER_HEAD
        ingredients(
            x, x, x,
            x, o, x,
            x, x, x
        )
    }
}