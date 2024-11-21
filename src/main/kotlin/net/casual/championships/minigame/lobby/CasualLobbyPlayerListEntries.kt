package net.casual.championships.minigame.lobby

import net.casual.arcade.resources.font.heads.PlayerHeadComponents
import net.casual.arcade.resources.font.spacing.SpacingFontResources
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.TeamUtils.color
import net.casual.arcade.visuals.tab.PlayerListEntries
import net.casual.championships.common.ui.tab.CasualPlayerListEntries
import net.casual.championships.duel.DuelRequester
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.scores.PlayerTeam

class CasualLobbyPlayerListEntries(
    private val minigame: CasualLobbyMinigame
): CasualPlayerListEntries(minigame) {
    override fun getTeams(server: MinecraftServer): Collection<PlayerTeam> {
        return this.minigame.getAllTeams().sortedWith(NAME_ORDER)
    }

    override fun createPlayerEntry(
        server: MinecraftServer,
        username: String,
        team: PlayerTeam,
        player: ServerPlayer?
    ): PlayerListEntries.Entry {
        if (player == null || !this.minigame.isDueling(player)) {
            return super.createPlayerEntry(server, username, team, player)
        }
        return PlayerListEntries.Entry.fromComponent(
            Component.empty()
                .append(DuelRequester.DUEL_PREFIX)
                .append(SpacingFontResources.spaced(2))
                .append(PlayerHeadComponents.getHeadOrDefault(player))
                .append(SpacingFontResources.spaced(2)).append(Component.literal(username).color(team).mini())
        )
    }
}