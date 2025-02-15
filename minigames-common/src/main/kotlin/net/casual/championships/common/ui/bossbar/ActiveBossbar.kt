package net.casual.championships.common.ui.bossbar

import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.stats.ArcadeStats
import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.ComponentUtils.shadowless
import net.casual.arcade.utils.TimeUtils.Ticks
import net.casual.arcade.utils.TimeUtils.formatHHMMSS
import net.casual.arcade.visuals.bossbar.CustomBossbar
import net.casual.championships.common.util.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.BossEvent

class ActiveBossbar(
    private val owner: Minigame
): CustomBossbar() {
    override fun getTitle(player: ServerPlayer): Component {
        val start = Component.empty()
            .append(SpacingFontResources.spaced(-5))
            .append(CommonComponents.Hud.BACKGROUND_40.copy().shadowless())
            .append(SpacingFontResources.spaced(-27))
            .append(Component.literal("%02d".format(this.owner.players.playingPlayerCount)).mini())
            .append(CommonComponents.Hud.PLAYER_COUNT)
            .append(SpacingFontResources.spaced(43))
        val end = Component.empty()
            .append(SpacingFontResources.spaced(37))
            .append(CommonComponents.Hud.BACKGROUND_40.copy().shadowless())
            .append(SpacingFontResources.spaced(-27))
            .append(Component.literal("%02d".format(this.owner.stats.getOrCreateStat(player, ArcadeStats.KILLS).value)).mini())
            .append(SpacingFontResources.spaced(1))
            .append(CommonComponents.Hud.KILLS_COUNT)
        val middle = CommonComponents.TIME_ELAPSED_BACKGROUNDED.generate(this.owner.uptime.Ticks.formatHHMMSS()).mini()
        return start.append(middle).append(end)
    }

    override fun getProgress(player: ServerPlayer): Float {
        return 1.0F
    }

    override fun getColour(player: ServerPlayer): BossEvent.BossBarColor {
        return BossEvent.BossBarColor.YELLOW
    }

    override fun getOverlay(player: ServerPlayer): BossEvent.BossBarOverlay {
        return BossEvent.BossBarOverlay.PROGRESS
    }
}