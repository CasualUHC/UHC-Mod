package net.casual.championships.common.ui.tab

import net.casual.arcade.minigame.Minigame
import net.casual.arcade.utils.ComponentUtils.color
import net.casual.arcade.utils.ComponentUtils.italicise
import net.casual.arcade.utils.ComponentUtils.literal
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.visuals.tab.PlayerListEntries
import net.casual.arcade.visuals.tab.VanillaPlayerListEntries
import net.minecraft.network.chat.Component

class SimpleCasualPlayerListEntries(
    val minigame: Minigame
): VanillaPlayerListEntries(
    { minigame.players.all },
    Comparator.comparing(minigame.players::isSpectating).thenComparing(DEFAULT_ORDER)
) {
    override fun getEntryAt(index: Int): PlayerListEntries.Entry {
        val entry = super.getEntryAt(index)
        val player = this.getPlayerAt(index)
        var display = Component.empty().append(entry.display).mini()
        if (this.minigame.players.isSpectating(player)) {
            display = player.scoreboardName.literal().color(0x919191).italicise().mini()
        }
        return entry.copy(display = display)
    }
}