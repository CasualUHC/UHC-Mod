package net.casual.championships.common.items

import net.casual.arcade.items.ItemStackFactory
import net.casual.championships.common.CommonMod.id
import net.casual.championships.common.util.CommonItems
import net.minecraft.world.item.ItemStack

object MinesweeperItems {
    private val modeller = ItemStackFactory.modeller(CommonItems.MINESWEEPER)
    val UNKNOWN by modeller.modelled(id("minesweeper/unknown"))
    val ONE by modeller.modelled(id("minesweeper/1"))
    val TWO by modeller.modelled(id("minesweeper/2"))
    val THREE by modeller.modelled(id("minesweeper/3"))
    val FOUR by modeller.modelled(id("minesweeper/4"))
    val FIVE by modeller.modelled(id("minesweeper/5"))
    val SIX by modeller.modelled(id("minesweeper/6"))
    val SEVEN by modeller.modelled(id("minesweeper/7"))
    val EIGHT by modeller.modelled(id("minesweeper/8"))
    val MINE by modeller.modelled(id("minesweeper/mine"))
    val FLAG by modeller.modelled(id("minesweeper/flag"))
    val FLAG_COUNTER by modeller.modelled(id("minesweeper/flag_counter"))

    private val numbers = arrayOf(ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT)

    fun of(tile: Int): ItemStack {
        return this.numbers.getOrNull(tile - 1)
            ?: throw IllegalArgumentException("Invalid tile")
    }
}