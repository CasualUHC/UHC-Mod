package net.casual.championships.common.ui.elements

import net.casual.arcade.minigame.utils.MinigameUtils.getMinigame
import net.casual.arcade.resources.font.heads.PlayerHeadComponents
import net.casual.arcade.utils.ComponentUtils
import net.casual.arcade.utils.ComponentUtils.literal
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.PlayerUtils.isSurvival
import net.casual.arcade.utils.PlayerUtils.player
import net.casual.arcade.utils.TeamUtils.color
import net.casual.arcade.visuals.elements.PlayerSpecificElement
import net.casual.arcade.visuals.sidebar.SidebarComponent
import net.casual.championships.common.util.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.scores.PlayerTeam

class TeammateElement(
    private val index: Int,
    private val buffer: Component,
    private val unavailable: Component
): PlayerSpecificElement<SidebarComponent> {
    private val none = SidebarComponent.withCustomScore(
        Component.empty().append(this.buffer).append(" - "),
        Component.empty().append(this.unavailable).append(this.buffer)
    )

    override fun get(player: ServerPlayer): SidebarComponent {
        val team = player.team
        val minigame = player.getMinigame()
        if (team == null || (minigame != null && minigame.teams.isTeamIgnored(team))) {
            return this.none
        }
        val players = team.players
        if (this.index >= players.size) {
            return this.none
        }

        val name: String
        val teammate: ServerPlayer?
        if (this.index == 0) {
            name = player.scoreboardName
            teammate = player
        } else {
            name = players.filter { it != player.scoreboardName }[this.index - 1]
            teammate = player.server.player(name)
        }

        val formatted = this.createTeammate(name, team)
        val score = if (teammate != null) {
            if (teammate.isSurvival && teammate.isAlive) {
                val health = " %04.1f".format(teammate.health / 2.0)
                health.literal().mini().append(ComponentUtils.space(1)).append(CommonComponents.Hud.HARDCORE_HEART)
            } else {
                Component.empty().append(this.unavailable)
            }
        } else {
            CommonComponents.Hud.NO_CONNECTION
        }
        return SidebarComponent.withCustomScore(formatted, score.append(this.buffer))
    }

    private fun createTeammate(name: String, team: PlayerTeam): MutableComponent {
        return Component.empty().apply {
            append(buffer)
            append(" ")
            append(PlayerHeadComponents.getHeadOrDefault(name))
            append(" ")
            append(name.literal().mini().color(team))
        }
    }
}