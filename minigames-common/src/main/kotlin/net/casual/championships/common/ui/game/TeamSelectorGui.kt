package net.casual.championships.common.ui.game

import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.utils.ComponentUtils.white
import net.casual.arcade.utils.ItemUtils.hideTooltip
import net.casual.arcade.utils.TeamUtils.getOnlinePlayers
import net.casual.arcade.visuals.screen.setSlot
import net.casual.championships.common.items.DisplayItems
import net.casual.championships.common.ui.CommonSimpleGui
import net.casual.championships.common.util.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.scores.PlayerTeam

class TeamSelectorGui(
    player: ServerPlayer,
    selections: List<Selection>
): CommonSimpleGui(MenuType.GENERIC_9x6, player, true) {
    init {
        this.title = Component.empty()
            .append(SpacingFontResources.spaced(-8))
            .append(CommonComponents.Gui.TEAM_SELECTOR.copy().white())

        var row = 1
        var column = 1
        for (selection in selections.take(12)) {
            val slot = row * 9 + column
            this.setSlot(slot, selection.display) { ->
                val profiles = selection.team.getOnlinePlayers().map(ServerPlayer::getGameProfile)
                val gui = PlayerSelectorGui(this.player, profiles)
                gui.setParent(this)
                gui.open()
            }
            column += 2
            if (column > 7) {
                column = 1
                row += 2
            }
        }

        this.setSlot(58, DisplayItems.RED_BACK.hideTooltip()) { ->
            this.close()
        }
    }

    data class Selection(val team: PlayerTeam, val display: ItemStack)
}

