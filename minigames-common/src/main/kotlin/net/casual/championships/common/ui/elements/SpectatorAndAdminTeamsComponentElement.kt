package net.casual.championships.common.ui.elements

import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.managers.MinigameTeamManager
import net.casual.arcade.resources.font.heads.PlayerHeadComponents
import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.utils.ComponentUtils
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.TeamUtils.getOnlineCount
import net.casual.arcade.utils.TeamUtils.getOnlinePlayers
import net.casual.arcade.visuals.elements.UniversalElement
import net.casual.championships.common.util.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.*

class SpectatorAndAdminTeamsComponentElement(
    private val minigame: Minigame
): UniversalElement<Optional<Component>> {
    override fun get(server: MinecraftServer): Optional<Component> {
        val teams = this.minigame.teams
        val component = if (!teams.hasSpectators()) {
            if (!teams.hasAdmins()) {
                return Optional.empty()
            }

            Component.empty()
                .append(CommonComponents.ADMINS.mini())
                .append("\n")
                .append(this.formatPlayerHeads(teams.getAdminTeam().getOnlinePlayers()))
        } else if (!teams.hasAdmins()) {
            Component.empty()
                .append(CommonComponents.SPECTATORS.mini())
                .append("\n")
                .append(this.formatPlayerHeads(teams.getSpectatorTeam().getOnlinePlayers()))
        } else {
            val spectators = teams.getSpectatorTeam().getOnlinePlayers()
            val admins = teams.getAdminTeam().getOnlinePlayers()

            val spectatorLength = this.calculateHeadsLength(spectators.size)
            val adminLength = this.calculateHeadsLength(admins.size)

            Component.empty()
                .append(CommonComponents.SPECTATORS.mini())
                .append(": ")
                .append(this.formatPlayerHeads(spectators))
                .append("\n")
                .append(ComponentUtils.widthDifferenceBetween(CommonComponents.SPECTATORS, CommonComponents.ADMINS))
                .append(CommonComponents.ADMINS.mini())
                .append(": ")
                .append(this.formatPlayerHeads(admins))
                .append(SpacingFontResources.spaced(spectatorLength - adminLength))

        }
        return Optional.of(component)
    }

    private fun formatPlayerHeads(players: Iterable<ServerPlayer>): Component {
        val component = Component.empty()
        val iter = players.iterator()
        for (player in iter) {
            component.append(PlayerHeadComponents.getHeadOrDefault(player))
            if (iter.hasNext()) {
                component.append(SpacingFontResources.spaced(4))
            }
        }
        return component
    }

    private fun MinigameTeamManager.hasSpectators(): Boolean {
        return this.hasSpectatorTeam() && this.getSpectatorTeam().getOnlineCount() > 0
    }

    private fun MinigameTeamManager.hasAdmins(): Boolean {
        return this.hasAdminTeam() && this.getAdminTeam().getOnlineCount() > 0
    }

    private fun calculateHeadsLength(heads: Int): Int {
        return heads * 8 + (heads - 1) * 5
    }
}