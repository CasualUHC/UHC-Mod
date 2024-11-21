package net.casual.championships.duel.ui

import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.ComponentUtils.white
import net.casual.arcade.utils.ComponentUtils.yellow
import net.casual.arcade.utils.ItemUtils
import net.casual.arcade.utils.ItemUtils.hideTooltip
import net.casual.arcade.utils.ItemUtils.named
import net.casual.arcade.visuals.screen.setSlot
import net.casual.championships.common.items.DisplayItems
import net.casual.championships.common.ui.CommonSimpleGui
import net.casual.championships.common.util.CommonComponents
import net.casual.championships.common.util.CommonItems
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.MenuType

class DuelPlayerSelectionGui(
    player: ServerPlayer,
    private val configuration: DuelConfigurationGui
): CommonSimpleGui(MenuType.GENERIC_9x6, player, true) {
    private var page = 0

    init {
        this.setParent(this.configuration)

        this.setSlot(58, DisplayItems.RED_BACK.hideTooltip()) { ->
            this.openParentOrClose()
        }

        this.loadPlayers()

        this.title = Component.empty()
            .append(SpacingFontResources.spaced(-8))
            .append(CommonComponents.Gui.PLAYER_SELECTOR.copy().white())
    }

    private fun loadPlayers() {
        this.clearPlayers()

        val players = this.configuration.getAvailablePlayers()
        val filtered = players.stream()
            .filter { it.uuid != this.player.uuid }
            .skip(this.page * 12L)
            .limit(12)
            .toList()

        // If all the players suddenly leave, we just jump
        // back to the first page to avoid many empty pages
        if (filtered.isEmpty() && this.page != 0) {
            this.page = 0
            this.loadPlayers()
            return
        }

        var row = 1
        var column = 1
        for (player in filtered) {
            val slot = row * 9 + column
            val head = ItemUtils.createPlayerHead(player, CommonItems.FORWARD_FACING_PLAYER_HEAD)
            if (this.configuration.isPlayerSelected(player.uuid)) {
                this.setSlot(slot - 9, DisplayItems.GREEN_HIGHLIGHT.hideTooltip())
            }
            val name = Component.literal(player.scoreboardName).yellow().mini()
            // TODO: Why is the name not working??!?!?!??
            this.setSlot(slot, head.named(name)) { _, _, _, _ ->
                if (this.configuration.toggleSelection(player.uuid)) {
                    this.setSlot(slot - 9, DisplayItems.GREEN_HIGHLIGHT.hideTooltip())
                } else {
                    this.clearSlot(slot - 9)
                }
            }
            column += 2
            if (column > 7) {
                column = 1
                row += 2
            }
        }

        if (this.page != 0) {
            this.setSlot(57, DisplayItems.RED_LEFT.hideTooltip()) { ->
                this.page -= 1
                this.loadPlayers()
            }
        } else {
            this.setSlot(57, DisplayItems.GREY_RED_LEFT.hideTooltip())
        }
        if ((this.page + 1) * 12 < players.size - 1) {
            this.setSlot(59, DisplayItems.RED_RIGHT.hideTooltip()) { ->
                this.page += 1
                this.loadPlayers()
            }
        } else {
            this.setSlot(59, DisplayItems.GREY_RED_RIGHT.hideTooltip())
        }
    }

    private fun clearPlayers() {
        repeat(3) { row ->
            repeat(4) { column ->
                val slot = (column * 2 + 1) + (row * 2 + 1) * 9
                this.clearSlot(slot)
                this.clearSlot(slot - 9)
            }
        }
    }
}