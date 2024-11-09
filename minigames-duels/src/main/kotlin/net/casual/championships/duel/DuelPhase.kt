package net.casual.championships.duel

import net.casual.arcade.minigame.phase.Phase
import net.casual.arcade.minigame.template.teleporter.EntityTeleporter.Companion.teleport
import net.casual.arcade.minigame.utils.MinigameUtils.countdown
import net.casual.arcade.scheduler.GlobalTickedScheduler
import net.casual.arcade.utils.ComponentUtils.literal
import net.casual.arcade.utils.PlayerUtils.sendTitle
import net.casual.arcade.utils.TeamUtils.color
import net.casual.arcade.utils.TeamUtils.getOnlinePlayers
import net.casual.arcade.utils.TimeUtils.Seconds
import net.casual.arcade.utils.resetToDefault
import net.casual.arcade.utils.set
import net.casual.championships.common.ui.bossbar.ActiveBossbar
import net.casual.championships.common.util.CommonComponents
import net.casual.championships.common.util.CommonStats
import net.minecraft.ChatFormatting
import net.minecraft.world.level.GameRules

internal const val INITIALIZING_ID = "initializing"
internal const val COUNTDOWN_ID = "countdown"
internal const val DUELING_ID = "dueling"
internal const val COMPLETE_ID = "complete"

enum class DuelPhase(
    override val id: String
): Phase<DuelMinigame> {
    Initializing(INITIALIZING_ID) {
        override fun start(minigame: DuelMinigame, previous: Phase<DuelMinigame>) {
            minigame.arena.area.replace()

            minigame.levels.setGameRules {
                resetToDefault()
                if (!minigame.duelSettings.naturalRegen) {
                    set(GameRules.RULE_NATURAL_REGENERATION, false)
                }
            }

            minigame.ui.addBossbar(ActiveBossbar(minigame))

            minigame.arena.teleporter.teleport(minigame.level, minigame.players.playing, minigame.duelSettings.teams)

            GlobalTickedScheduler.later {
                minigame.setPhase(Countdown)
            }
        }
    },
    Countdown(COUNTDOWN_ID) {
        override fun start(minigame: DuelMinigame, previous: Phase<DuelMinigame>) {
            minigame.settings.freezeEntities.set(true)
            minigame.ui.countdown.countdown(minigame).then {
                minigame.setPhase(Dueling)
            }
        }
    },
    Dueling(DUELING_ID) {
        override fun start(minigame: DuelMinigame, previous: Phase<DuelMinigame>) {
            minigame.settings.freezeEntities.set(false)
        }
    },
    Complete(COMPLETE_ID) {
        override fun start(minigame: DuelMinigame, previous: Phase<DuelMinigame>) {
            var winner = if (minigame.duelSettings.teams) {
                val winners = minigame.teams.getPlayingTeams().firstOrNull()
                if (winners != null) {
                    for (winner in winners.getOnlinePlayers()) {
                        minigame.stats.getOrCreateStat(winner, CommonStats.WON).modify { true }
                    }
                }
                winners?.formattedDisplayName
            } else {
                val winner = minigame.players.playing.firstOrNull()
                if (winner != null) {
                    minigame.stats.getOrCreateStat(winner, CommonStats.WON).modify { true }
                    val named = winner.scoreboardName.literal()
                    val team = winner.team
                    if (team != null) {
                        named.color(team)
                    }
                    named
                } else {
                    null
                }
            }
            if (winner == null) {
                CasualDuelMod.logger.warn("Couldn't find winner for duel!")
                winner = "Unknown".literal().withStyle(ChatFormatting.OBFUSCATED)
            }

            val title = CommonComponents.GAME_WON.generate(winner)
            for (player in minigame.players) {
                player.sendTitle(title)
            }

            minigame.scheduler.schedule(10.Seconds) {
                minigame.complete()
            }

            minigame.stats.freeze()
        }
    }
}