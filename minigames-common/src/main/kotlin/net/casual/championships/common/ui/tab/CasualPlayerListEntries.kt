package net.casual.championships.common.ui.tab

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.resources.font.heads.PlayerHeadComponents
import net.casual.arcade.resources.font.heads.PlayerHeadFont
import net.casual.arcade.utils.ComponentUtils
import net.casual.arcade.utils.ComponentUtils.color
import net.casual.arcade.utils.ComponentUtils.greyscale
import net.casual.arcade.utils.ComponentUtils.italicise
import net.casual.arcade.utils.ComponentUtils.mini
import net.casual.arcade.utils.TeamUtils.color
import net.casual.arcade.visuals.tab.PlayerListEntries
import net.casual.arcade.visuals.tab.TeamListEntries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.scores.PlayerTeam
import java.time.Duration
import java.util.concurrent.CompletableFuture

open class CasualPlayerListEntries(
    private val minigame: Minigame
): TeamListEntries() {
    override fun getTeams(server: MinecraftServer): Collection<PlayerTeam> {
        return this.minigame.teams.getAllNonSpectatorOrAdminTeams().sortedWith(NAME_ORDER)
    }

    override fun formatTeamName(server: MinecraftServer, team: PlayerTeam): MutableComponent {
        return super.formatTeamName(server, team).color(team).mini()
    }

    override fun createPlayerEntry(
        server: MinecraftServer,
        username: String,
        team: PlayerTeam,
        player: ServerPlayer?
    ): PlayerListEntries.Entry {
        val name = Component.literal(username)
        val head = if (player != null) {
            if (this.minigame.players.isPlaying(player)) {
                name.color(team)
                PlayerHeadComponents.getHeadOrDefault(player)
            } else {
                name.italicise().color(0x919191)
                GREYSCALE_CACHE.get(username).getNow(PlayerHeadFont.STEVE_HEAD)
            }
        } else {
            name.color(0x808080)
            GREYSCALE_CACHE.get(username).getNow(PlayerHeadFont.STEVE_HEAD)
        }
        return PlayerListEntries.Entry.fromComponent(
            Component.empty().append(head).append(ComponentUtils.space(2)).append(name.mini())
        )
    }

    companion object {
        private val GREYSCALE_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(60))
            .build(object: CacheLoader<String, CompletableFuture<Component>>() {
                override fun load(key: String): CompletableFuture<Component> {
                    return PlayerHeadComponents.getHead(key).thenApply { it.greyscale() }
                }
            })
    }
}