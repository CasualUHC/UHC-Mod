package net.casual.championships.common.ui.game

import com.mojang.authlib.GameProfile
import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.ComponentUtils.white
import net.casual.arcade.utils.ComponentUtils.yellow
import net.casual.arcade.utils.ItemUtils
import net.casual.arcade.utils.ItemUtils.hideTooltip
import net.casual.arcade.utils.ItemUtils.named
import net.casual.arcade.utils.PlayerUtils.location
import net.casual.arcade.utils.PlayerUtils.teleportTo
import net.casual.arcade.visuals.screen.setSlot
import net.casual.championships.common.items.DisplayItems
import net.casual.championships.common.ui.CommonSimpleGui
import net.casual.championships.common.util.CommonComponents
import net.casual.championships.common.util.CommonItems
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.MenuType

class PlayerSelectorGui(
    player: ServerPlayer,
    profiles: List<GameProfile>
): CommonSimpleGui(MenuType.GENERIC_9x6, player, true) {
    init {
        this.title = Component.empty()
            .append(SpacingFontResources.spaced(-8))
            .append(CommonComponents.Gui.TEAM_PLAYER_SELECTOR.copy().white())

        for ((i, profile) in profiles.take(5).withIndex()) {
            val head = ItemUtils.createPlayerHead(profile, CommonItems.FORWARD_FACING_PLAYER_HEAD)
            val name = Component.literal(profile.name).yellow().mini()
            this.setSlot(47 + i, head.named(name)) { ->
                val selected = this.player.server.playerList.getPlayer(profile.id)
                if (selected != null) {
                    this.player.teleportTo(selected.location)
                }
            }
        }

        this.setSlot(58, DisplayItems.RED_BACK.hideTooltip()) { ->
            this.openParentOrClose()
        }
    }
}

