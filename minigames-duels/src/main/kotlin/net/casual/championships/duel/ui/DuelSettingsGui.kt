package net.casual.championships.duel.ui

import net.casual.arcade.minigame.settings.display.MenuGameSetting
import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.utils.ComponentUtils.white
import net.casual.arcade.utils.ItemUtils.hideTooltip
import net.casual.arcade.visuals.screen.setSlot
import net.casual.championships.common.items.DisplayItems
import net.casual.championships.common.ui.CommonSimpleGui
import net.casual.championships.common.util.CommonComponents
import net.casual.championships.duel.DuelSettings
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.MenuType

class DuelSettingsGui(
    player: ServerPlayer,
    private val settings: DuelSettings,
    private val configuration: DuelConfigurationGui
): CommonSimpleGui(MenuType.GENERIC_9x6, player, true) {
    init {
        this.setParent(this.configuration)

        this.title = Component.empty()
            .append(SpacingFontResources.spaced(-8))
            .append(CommonComponents.Gui.DUEL_SETTINGS.copy().white())

        this.createHorizontalSetting(19, this.settings.displayableTeams)
        this.createHorizontalSetting(28, this.settings.displayableGlowing)
        this.createHorizontalSetting(37, this.settings.displayableNaturalRegen)
        this.createHorizontalSetting(46, this.settings.displayablePlayerDropHeads)

        this.createVerticalSetting(23, this.settings.displayableHealth)
        this.createVerticalSetting(24, this.settings.displayableArena)
        this.createVerticalSetting(25, this.settings.displayableArenaSize)

        this.setSlot(58, DisplayItems.RED_BACK.hideTooltip()) { ->
            this.openParentOrClose()
        }
    }

    private fun createHorizontalSetting(slot: Int, setting: MenuGameSetting<*>) {
        this.setSlot(slot, setting.display)
        for ((i, option) in setting.options.withIndex()) {
            this.setSlot(slot + i + 1, option)
        }
    }

    private fun createVerticalSetting(slot: Int, setting: MenuGameSetting<*>) {
        this.setSlot(slot, setting.display)
        for ((i, option) in setting.options.withIndex()) {
            this.setSlot(slot + (i + 1) * 9, option)
        }
    }
}