package net.casual.championships.uhc

import net.casual.arcade.dimensions.level.vanilla.VanillaDimension
import net.casual.arcade.minigame.phase.Phase
import net.casual.arcade.minigame.task.impl.BossbarTask.Companion.then
import net.casual.arcade.minigame.task.impl.BossbarTask.Companion.withDuration
import net.casual.arcade.minigame.task.impl.MinigameTask
import net.casual.arcade.minigame.task.impl.PhaseChangeTask
import net.casual.arcade.minigame.template.teleporter.EntityTeleporter.Companion.teleport
import net.casual.arcade.scheduler.GlobalTickedScheduler
import net.casual.arcade.utils.ComponentUtils.gold
import net.casual.arcade.utils.ComponentUtils.literal
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.ComponentUtils.red
import net.casual.arcade.utils.PlayerUtils.sendSound
import net.casual.arcade.utils.PlayerUtils.sendTitle
import net.casual.arcade.utils.TeamUtils.color
import net.casual.arcade.utils.TeamUtils.getOnlinePlayers
import net.casual.arcade.utils.TimeUtils.Minutes
import net.casual.arcade.utils.TimeUtils.Seconds
import net.casual.arcade.utils.TimeUtils.Ticks
import net.casual.arcade.utils.impl.Location
import net.casual.arcade.utils.impl.Sound
import net.casual.arcade.utils.resetToDefault
import net.casual.arcade.utils.set
import net.casual.arcade.utils.teleportTo
import net.casual.championships.common.task.GlowingBossbarTask
import net.casual.championships.common.task.GracePeriodBossbarTask
import net.casual.championships.common.util.CommonComponents
import net.casual.championships.common.util.CommonSounds
import net.casual.championships.common.util.CommonUI
import net.casual.championships.common.util.CommonUI.broadcastGame
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.level.GameRules

internal const val INITIALIZING_ID = "initializing"
internal const val GRACE_ID = "grace"
internal const val BORDER_MOVING_ID = "border_moving"
internal const val BORDER_FINISHED_ID = "border_finished"
internal const val GAME_OVER_ID = "game_over"

enum class UHCPhase(
    override val id: String
): Phase<UHCMinigame> {
    Initializing(INITIALIZING_ID) {
        override fun start(minigame: UHCMinigame, previous: Phase<UHCMinigame>) {
            minigame.settings.canPvp.set(false)
            minigame.settings.tickFreezeOnPause.set(true)
            minigame.levels.all().forEach { it.dayTime = 0 }
            minigame.resetWorldBorders()

            val (level, _) = when (minigame.settings.startingDimension) {
                VanillaDimension.Overworld -> minigame.overworld to null
                VanillaDimension.Nether -> minigame.nether to 120
                VanillaDimension.End -> minigame.end to null
            }
            UHCSpreadTeleporter.teleport(level, minigame.players.playing, true)

            for (player in minigame.players.spectating) {
                if (player.level() != level) {
                    player.teleportTo(Location.of(0.0, 200.0, 0.0, 0.0F, 0.0F, level))
                }
            }

            GlobalTickedScheduler.later {
                minigame.setPhase(Grace)
            }
        }

        override fun initialize(minigame: UHCMinigame) {
            // Fantasy does not save game rule data, we must always reset it.
            minigame.levels.setGameRules {
                resetToDefault()
                set(GameRules.RULE_NATURAL_REGENERATION, false)
                set(GameRules.RULE_DOINSOMNIA, false)
                set(GameRules.RULE_DO_IMMEDIATE_RESPAWN, true)
            }

            minigame.teams.hideNameTags()

            minigame.ui.removeAllNameTags()
            minigame.ui.addNameTag(CommonUI.createPlayingNameTag())
            minigame.ui.addNameTag(CommonUI.createPlayingHealthTag())
        }
    },
    Grace(GRACE_ID) {
        override fun start(minigame: UHCMinigame, previous: Phase<UHCMinigame>) {
            minigame.settings.isChatGlobal = false
            minigame.settings.mobsWithNoAIAreFlammable = true
            val duration = minigame.settings.gracePeriod
            val task = GracePeriodBossbarTask(minigame)
                .withDuration(duration - 1.Ticks)
                .then(PhaseChangeTask(minigame, BorderMoving))
            minigame.scheduler.schedulePhasedCancellable(duration, task).runIfCancelled()

            val minutes = duration.minutes
            minigame.chat.broadcastGame(CommonComponents.BORDER_INITIAL_GRACE.generate(minutes).gold().mini())
        }

        override fun end(minigame: UHCMinigame, next: Phase<UHCMinigame>) {
            if (this < next) {
                minigame.chat.broadcastGame(
                    CommonComponents.BORDER_GRACE_OVER.red().mini(),
                    sound = Sound(CommonSounds.GAME_BORDER_MOVING)
                )
                minigame.settings.canPvp.set(true)
            }
        }
    },
    BorderMoving(BORDER_MOVING_ID) {
        override fun start(minigame: UHCMinigame, previous: Phase<UHCMinigame>) {
            minigame.startWorldBorders()
        }
    },
    BorderFinished(BORDER_FINISHED_ID) {
        override fun start(minigame: UHCMinigame, previous: Phase<UHCMinigame>) {
            val task = GlowingBossbarTask(minigame)
                .withDuration(2.Minutes)
                .then(MinigameTask(minigame, UHCMinigame::onBorderFinish))
            minigame.scheduler.schedulePhasedCancellable(2.Minutes, task).runIfCancelled()
        }

        override fun end(minigame: UHCMinigame, next: Phase<UHCMinigame>) {
            if (this < next) {
                minigame.settings.canPvp.set(false)
            }
        }
    },
    GameOver(GAME_OVER_ID) {
        override fun start(minigame: UHCMinigame, previous: Phase<UHCMinigame>) {
            minigame.settings.isChatGlobal = true
            minigame.settings.canTakeDamage.set(false)

            val teams = minigame.teams.getPlayingTeams()
            if (teams.size != 1) {
                val message = "Expected to have one team remaining on game over!"
                minigame.chat.broadcastTo(message.literal(), minigame.players.admins)
                return
            }
            val team = teams.first()

            minigame.uhcAdvancements.grantFinalAdvancements(team.getOnlinePlayers())
            minigame.winners.clear()
            minigame.winners.addAll(team.players)

            for (player in minigame.players) {
                player.sendTitle(CommonComponents.GAME_WON.generate(team.name).color(team).mini())
            }

            // TODO: Better winning screen
            val winTask = MinigameTask(minigame) {
                for (player in it.players) {
                    player.sendSound(SoundEvents.FIREWORK_ROCKET_BLAST, volume = 0.5F)
                    player.sendSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, volume = 0.5F)
                    player.sendSound(SoundEvents.FIREWORK_ROCKET_BLAST_FAR, volume = 0.5F)
                }
                it.scheduler.schedulePhased(6.Ticks) {
                    for (player in it.players) {
                        player.sendSound(SoundEvents.FIREWORK_ROCKET_SHOOT, volume = 0.5F)
                        player.sendSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, volume = 0.5F)
                        player.sendSound(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, volume = 0.5F)
                    }
                }
            }
            minigame.scheduler.schedulePhasedInLoop(0.Ticks, 4.Ticks, 100.Ticks, winTask)

            minigame.scheduler.schedulePhased(20.Seconds, MinigameTask(minigame) {
                minigame.complete()
            })

            minigame.stats.freeze()
        }
    }
}