package net.casual.championships.common.ui.elements

import net.casual.arcade.resources.font.heads.PlayerHeadComponents
import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.utils.ComponentUtils.bold
import net.casual.arcade.utils.ComponentUtils.italicise
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.PlayerUtils.isSurvival
import net.casual.arcade.utils.PlayerUtils.player
import net.casual.arcade.utils.TeamUtils.color
import net.casual.arcade.visuals.sidebar.SidebarComponent
import net.casual.arcade.visuals.sidebar.SidebarComponents
import net.casual.championships.common.util.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.scores.PlayerTeam

class TeammatesSidebarElements(
    private val buffer: Component,
    private val playerBuffer: Component,
    private val health: Boolean
) {
    fun addTeammates(
        player: ServerPlayer,
        components: SidebarComponents<SidebarComponent>
    ) {
        val teammates = linkedSetOf(player.scoreboardName)
        val team = player.team
        if (team != null) {
            teammates.addAll(team.players)
            components.addRow(SidebarComponent.withNoScore(
                Component.empty()
                    .append(this.buffer)
                    .append(Component.literal("Team: ").bold())
                    .append(Component.literal(team.name).color(team).italicise())
                    .mini()
            ))
        }

        for (username in teammates) {
            components.addRow(this.createTeammateComponent(player.server, username, team))
        }
    }

    private fun createTeammateComponent(server: MinecraftServer, username: String, team: PlayerTeam?): SidebarComponent {
        val formatted = Component.empty()
            .append(this.buffer)
            .append(this.playerBuffer)
            .append(PlayerHeadComponents.getHeadOrDefault(username))
            .append(" ")
            .append(Component.literal(username).mini().color(team))
        val teammate = server.player(username)
            ?: return SidebarComponent.withCustomScore(formatted, CommonComponents.Hud.NO_CONNECTION.copy().append(this.buffer))

        if (!this.health) {
            return SidebarComponent.withCustomScore(formatted.append(this.buffer), Component.empty())
        }
        if (!teammate.isSurvival || !teammate.isAlive) {
            return SidebarComponent.withCustomScore(formatted, CommonComponents.Hud.UNAVAILABLE.copy().append(this.buffer))
        }
        val health = " %04.1f".format(teammate.health / 2.0)
        val score = Component.literal(health).mini().append(SpacingFontResources.spaced(1)).append(CommonComponents.Hud.HARDCORE_HEART)
        return SidebarComponent.withCustomScore(formatted, score.append(this.buffer))
    }
}