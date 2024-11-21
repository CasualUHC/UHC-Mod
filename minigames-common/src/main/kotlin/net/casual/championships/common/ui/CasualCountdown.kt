package net.casual.championships.common.ui

import net.casual.arcade.utils.ComponentUtils.bold
import net.casual.arcade.utils.ComponentUtils.gold
import net.casual.arcade.utils.ComponentUtils.lime
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.ComponentUtils.red
import net.casual.arcade.utils.ComponentUtils.yellow
import net.casual.arcade.utils.PlayerUtils.sendSound
import net.casual.arcade.utils.PlayerUtils.sendTitle
import net.casual.arcade.utils.PlayerUtils.setTitleAnimation
import net.casual.arcade.utils.impl.Sound
import net.casual.arcade.visuals.countdown.TitledCountdown
import net.casual.championships.common.util.CommonComponents
import net.casual.championships.common.util.CommonSounds
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

object CasualCountdown: TitledCountdown {
    override fun getCountdownTitle(current: Int): Component {
        return CommonComponents.STARTING_IN.generate("").mini()
    }

    override fun getCountdownSubtitle(current: Int): Component {
        val subtitle = Component.empty().append("▶ ").append(Component.literal(current.toString()).mini()).append(" ◀")
        when (current) {
            3 -> subtitle.red()
            2 -> subtitle.yellow()
            1 -> subtitle.lime()
        }
        return subtitle
    }

    override fun getCountdownSound(current: Int): Sound? {
        if (current <= 3) {
            return Sound(CommonSounds.COUNTDOWN_TICK_HIGH)
        }
        if (current <= 10) {
            return Sound(CommonSounds.COUNTDOWN_TICK_NORMAL)
        }
        return null
    }

    override fun afterCountdown(players: Collection<ServerPlayer>) {
        val final = Sound(CommonSounds.COUNTDOWN_TICK_END)
        for (player in players) {
            player.setTitleAnimation()
            player.sendTitle(CommonComponents.GOOD_LUCK.gold().bold().mini(), Component.empty())
            player.sendSound(final)
        }
    }
}