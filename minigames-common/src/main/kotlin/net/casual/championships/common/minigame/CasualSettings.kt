package net.casual.championships.common.minigame

import eu.pb4.sgui.api.gui.GuiInterface
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.settings.MinigameSettings
import net.casual.arcade.minigame.settings.display.DisplayableSettingsDefaults
import net.casual.arcade.minigame.settings.display.MenuGameSetting
import net.casual.arcade.minigame.settings.display.MenuGameSettingBuilder
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.ItemUtils.named
import net.casual.arcade.visuals.screen.SelectionGuiBuilder
import net.casual.arcade.visuals.screen.SelectionGuiStyle
import net.casual.championships.common.items.DisplayItems.CROSS
import net.casual.championships.common.items.DisplayItems.CROSS_SELECTED
import net.casual.championships.common.items.DisplayItems.TICK
import net.casual.championships.common.items.DisplayItems.TICK_SELECTED
import net.casual.championships.common.util.CommonComponents.DISABLE
import net.casual.championships.common.util.CommonComponents.DISABLED
import net.casual.championships.common.util.CommonComponents.ENABLE
import net.casual.championships.common.util.CommonComponents.ENABLED
import net.casual.championships.common.util.CommonScreens
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

open class CasualSettings(
    minigame: Minigame,
    defaults: DisplayableSettingsDefaults = Defaults(Component.literal("Casual Minigame Settings"))
): MinigameSettings(minigame, defaults) {
    open class Defaults(title: Component): DisplayableSettingsDefaults() {
        private val components = CommonScreens.named(title)

        override fun createSettingsGuiBuilder(player: ServerPlayer): SelectionGuiBuilder {
            return SelectionGuiBuilder(player, this.components)
                .style(SelectionGuiStyle.centered(5, 3))
        }

        override fun createOptionsGuiBuilder(parent: GuiInterface, setting: MenuGameSetting<*>): SelectionGuiBuilder {
            return SelectionGuiBuilder(parent, CommonScreens.named(setting.display.hoverName))
                .style(SelectionGuiStyle.centered(setting.optionCount))
        }

        override fun options(builder: MenuGameSettingBuilder<Boolean>, enabled: ItemStack, disabled: ItemStack) {
            builder.option("enabled", TICK_SELECTED.named(ENABLED.mini()), true) { setting, _, _ ->
                if (setting.get()) TICK_SELECTED.named(ENABLED.mini()) else TICK.named(ENABLE.mini())
            }
            builder.option("disabled", CROSS_SELECTED.named(DISABLED.mini()), false) { setting, _, _ ->
                if (setting.get()) CROSS.named(DISABLE.mini()) else CROSS_SELECTED.named(DISABLED.mini())
            }
        }
    }
}