package net.casual.championships.uhc

import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.GuiElement
import eu.pb4.sgui.api.gui.HotbarGui
import net.casual.arcade.scheduler.GlobalTickedScheduler
import net.casual.arcade.utils.ScreenUtils.setSlot
import net.casual.championships.common.util.CommonUI
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class UHCSpectatorHotbar(
    player: ServerPlayer,
    private val uhc: UHCMinigame
): HotbarGui(player) {
    private val maps = ArrayList<GuiElement>()

    init {
        for ((i, map) in this.uhc.mapRenderer.getMaps().withIndex()) {
            val element = this.createMapGuiElement(map, i)
            this.maps.add(i, element)
            this.addSlot(element)
        }
        this.setSlot(8, ItemStack(Items.PLAYER_HEAD)) { ->
            val gui = CommonUI.createTeamSelectionGui(this.uhc, this.player)
            gui.setParent(this)
            gui.open()
        }
    }

    override fun canPlayerClose(): Boolean {
        return false
    }

    private fun createMapGuiElement(map: ItemStack, index: Int): GuiElement {
        return GuiElement(map) { _, type, _, _ ->
            if (type == ClickType.OFFHAND_SWAP) {
                GlobalTickedScheduler.later {
                    val current = this.maps[index]
                    val offhand = this.getSlot(9)?.itemStack ?: ItemStack.EMPTY
                    if (current.itemStack.isEmpty) {
                        current.itemStack = offhand
                        this.setSlot(9, ItemStack.EMPTY)
                    } else if (offhand.isEmpty) {
                        this.setSlot(9, current.itemStack)
                        current.itemStack = ItemStack.EMPTY
                    }
                }
            }
        }
    }
}