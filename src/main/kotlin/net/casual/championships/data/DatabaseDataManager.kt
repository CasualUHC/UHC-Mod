package net.casual.championships.data

import com.mojang.authlib.GameProfile
import kotlinx.coroutines.*
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.minigame.stats.ArcadeStats
import net.casual.arcade.utils.TeamUtils.getHexColor
import net.casual.arcade.utils.TeamUtils.setHexColor
import net.casual.arcade.utils.TimeUtils.Ticks
import net.casual.championships.CasualMod
import net.casual.championships.common.util.CommonStats
import net.casual.championships.duel.DuelMinigame
import net.casual.championships.uhc.UHCMinigame
import net.casual.championships.uhc.UHCStats
import net.casual.database.*
import net.casual.database.stats.DuelPlayerStats
import net.casual.database.stats.PlayerStats
import net.casual.database.stats.UHCPlayerStats
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.Team
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull
import net.casual.database.Minigame as DatabaseMinigame

class DatabaseDataManager(
    eventName: String,
    private val database: CasualDatabase
): DataManager {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val currentEvent = this.database.transaction { createEvent(eventName) }

    override fun getParticipants(): CompletableFuture<Set<GameProfile>> {
        val future = CompletableFuture<Set<GameProfile>>()
        this.asyncTransaction {
            val profiles = HashSet<GameProfile>()
            for (player in this.database.getDiscordPlayers()) {
                if (player.team != null) {
                    profiles.add(GameProfile(player.id.value, player.name))
                }
            }
            future.complete(profiles)
        }
        return future
    }

    override fun createTeams(server: MinecraftServer): CompletableFuture<Collection<PlayerTeam>> {
        val future = CompletableFuture<Collection<PlayerTeam>>()
        val scoreboard = server.scoreboard
        this.asyncTransaction {
            val teams = this.database.getDiscordTeams().map { it to it.players.toList() }
            server.execute {
                val created = ArrayList<PlayerTeam>(teams.size)
                for ((team, players) in teams) {
                    created.add(this.createTeam(team, players, scoreboard))
                }
                future.complete(created)
            }
        }
        return future
    }

    override fun reloadTeams(server: MinecraftServer): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        val scoreboard = server.scoreboard
        this.asyncTransaction {
            val teams = this.database.getDiscordTeams().map { it to it.players.toList() }
            server.execute {
                for ((team, players) in teams) {
                    this.reloadTeam(team, players, scoreboard)
                }
                future.complete(null)
            }
        }
        return future
    }

    override fun syncUHCData(uhc: UHCMinigame) {
        val participants = uhc.players.allProfiles.map {
            uhc.server.scoreboard.getPlayersTeam(it.name) to it
        }
        this.asyncTransaction {
            CasualMod.logger.info("Synchronizing uhc stats for ${uhc.uuid}")

            val databaseMinigame = this.getOrCreateMinigame(uhc)
            for ((team, profile) in participants) {
                team ?: continue
                val tracker = uhc.stats.getOrCreateTracker(profile.id)
                val player = this.getOrCreateMinigamePlayer(profile.id, team, databaseMinigame)

                this.syncPlayerAdvancements(uhc, player)
                this.getOrCreatePlayerStats(UHCPlayerStats, player) {
                    won = tracker.getStatValueOrDefault(CommonStats.WON)
                    died = tracker.getStatValueOrDefault(ArcadeStats.DEATHS) > 0
                    kills = tracker.getStatValueOrDefault(ArcadeStats.KILLS)
                    damageTaken = tracker.getStatValueOrDefault(ArcadeStats.DAMAGE_TAKEN)
                    damageDealt = tracker.getStatValueOrDefault(ArcadeStats.DAMAGE_DEALT)
                    damageHealed = tracker.getStatValueOrDefault(ArcadeStats.DAMAGE_HEALED)
                    headsConsumed = tracker.getStatValueOrDefault(UHCStats.HEADS_CONSUMED)
                    aliveTime = tracker.getStatValueOrDefault(CommonStats.ALIVE_TIME).Ticks.duration
                    crouchTime = tracker.getStatValueOrDefault(CommonStats.CROUCH_TIME).Ticks.duration
                    jumps = tracker.getStatValueOrDefault(CommonStats.JUMPS)
                    relogs = tracker.getStatValueOrDefault(ArcadeStats.RELOGS)
                    blocksMined = tracker.getStatValueOrDefault(CommonStats.BLOCKS_MINED)
                    blocksPlaced = tracker.getStatValueOrDefault(CommonStats.BLOCKS_PLACED)
                }
            }
        }
    }

    override fun syncDuelData(duel: DuelMinigame) {
        val participants = duel.players.allProfiles.map {
            duel.server.scoreboard.getPlayersTeam(it.name) to it
        }
        this.asyncTransaction {
            CasualMod.logger.info("Synchronizing duel stats for ${duel.uuid}")
            val databaseMinigame = this.getOrCreateMinigame(duel)
            for ((team, profile) in participants) {
                team ?: continue
                val tracker = duel.stats.getOrCreateTracker(profile.id)
                val player = this.getOrCreateMinigamePlayer(profile.id, team, databaseMinigame)

                this.syncPlayerAdvancements(duel, player)
                this.getOrCreatePlayerStats(DuelPlayerStats, player) {
                    won = tracker.getStatValueOrDefault(CommonStats.WON)
                    kills = tracker.getStatValueOrDefault(ArcadeStats.KILLS)
                    damageTaken = tracker.getStatValueOrDefault(ArcadeStats.DAMAGE_TAKEN)
                    damageDealt = tracker.getStatValueOrDefault(ArcadeStats.DAMAGE_DEALT)
                    damageHealed = tracker.getStatValueOrDefault(ArcadeStats.DAMAGE_HEALED)
                }
            }
        }
    }

    override fun close() {
        this.database.close()
        this.coroutineScope.cancel()
    }

    private fun asyncTransaction(statement: (Transaction) -> Unit) {
        this.coroutineScope.launch {
            database.transaction(statement)
        }
    }

    private fun syncPlayerAdvancements(minigame: Minigame, minigamePlayer: MinigamePlayer) {
        val advancements = minigame.data.getAdvancements(minigamePlayer.uuid)
        for (holder in advancements) {
            val minigameAdvancement = this.getOrCreateAdvancement(minigame, holder) ?: continue
            MinigameAdvancementAward.new {
                advancement = minigameAdvancement
                player = minigamePlayer
            }
        }
    }

    private fun getOrCreateAdvancement(minigame: Minigame, advancement: AdvancementHolder): MinigameAdvancement? {
        val type = minigame.id.toString()
        val id = advancement.id.toString()
        val minigameAdvancement = MinigameAdvancement.find {
            (MinigameAdvancements.advancementId eq id) and (MinigameAdvancements.minigameType eq type)
        }.singleOrNull()
        if (minigameAdvancement != null) {
            return minigameAdvancement
        }

        val display = advancement.value.display.getOrNull() ?: return null
        return MinigameAdvancement.new {
            advancementId = id
            minigameType = type
            displayItem = BuiltInRegistries.ITEM.getKey(display.icon.item).toString()
            title = display.title.string
        }
    }

    private fun <T: PlayerStats> getOrCreatePlayerStats(
        clazz: IntEntityClass<T>,
        player: MinigamePlayer,
        modifier: T.() -> Unit
    ) {
        val stats = clazz.findById(player.id.value)
        if (stats != null) {
            stats.modifier()
            return
        }
        clazz.new(player.id.value, modifier)
    }

    private fun getOrCreateMinigamePlayer(
        playerUUID: UUID,
        playerTeam: PlayerTeam,
        databaseMinigame: DatabaseMinigame
    ): MinigamePlayer {
        val eventPlayer = this.getOrCreateEventPlayer(playerUUID, playerTeam)
        val minigamePlayer = MinigamePlayer.find {
            (MinigamePlayers.player eq eventPlayer.id) and (MinigamePlayers.minigame eq databaseMinigame.id)
        }.singleOrNull()
        if (minigamePlayer != null) {
            return minigamePlayer
        }
        return MinigamePlayer.new {
            player = eventPlayer
            minigame = databaseMinigame
        }
    }

    private fun getOrCreateMinigame(minigame: Minigame): DatabaseMinigame {
        val databaseMinigame = DatabaseMinigame.findById(minigame.uuid)
        if (databaseMinigame != null) {
            return databaseMinigame
        }
        return DatabaseMinigame.new {
            type = minigame.id.toString()
            startTime = minigame.data.startTime
            endTime = minigame.data.endTime
            event = currentEvent
        }
    }

    private fun getOrCreateEventPlayer(playerUUID: UUID, playerTeam: PlayerTeam): EventPlayer {
        val eventTeam = this.getOrCreateEventTeam(playerTeam)
        val player = EventPlayer.find {
            (EventPlayers.uuid eq playerUUID) and (EventPlayers.team eq eventTeam.id)
        }.singleOrNull()
        if (player != null) {
            return player
        }
        return EventPlayer.new {
            uuid = playerUUID
            team = eventTeam
        }
    }

    private fun getOrCreateEventTeam(playerTeam: PlayerTeam): EventTeam {
        val team = EventTeam.find {
            (EventTeams.name eq playerTeam.name) and (EventTeams.event eq currentEvent.id)
        }.singleOrNull()
        if (team != null) {
            return team
        }
        return EventTeam.new {
            name = playerTeam.name
            event = currentEvent
            color = playerTeam.getHexColor() ?: 0xFFFFFF
        }
    }

    private fun createTeam(discordTeam: DiscordTeam, players: List<DiscordPlayer>, scoreboard: Scoreboard): PlayerTeam {
        var team = scoreboard.getPlayerTeam(discordTeam.name)
        if (team == null) {
            team = scoreboard.addPlayerTeam(discordTeam.name)!!
        }
        for (player in team.players.toList()) {
            scoreboard.removePlayerFromTeam(player, team)
        }

        team.playerPrefix = Component.literal("[${discordTeam.prefix}] ")
        team.setHexColor(discordTeam.color)

        for (player in players) {
            scoreboard.addPlayerToTeam(player.name, team)
        }
        team.isAllowFriendlyFire = false
        team.collisionRule = Team.CollisionRule.ALWAYS
        return team
    }

    private fun reloadTeam(discordTeam: DiscordTeam, players: List<DiscordPlayer>, scoreboard: Scoreboard) {
        val team = scoreboard.getPlayerTeam(discordTeam.name)
        if (team == null) {
            CasualMod.logger.error("Failed to reload team ${discordTeam.name}, no such team available")
            return
        }
        for (player in team.players.toList()) {
            scoreboard.removePlayerFromTeam(player, team)
        }
        for (player in players) {
            scoreboard.addPlayerToTeam(player.name, team)
        }
    }

    private fun createEvent(eventName: String): Event {
        val event = Event.find { Events.name eq eventName }.singleOrNull()
        if (event != null) {
            return event
        }
        return Event.new {
            name = eventName
        }
    }
}