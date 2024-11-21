package net.casual.championships.data

import com.mojang.authlib.GameProfile
import net.casual.arcade.minigame.Minigame
import net.casual.arcade.utils.JsonUtils
import net.casual.championships.CasualMod
import net.casual.championships.duel.DuelMinigame
import net.casual.championships.uhc.UHCMinigame
import net.casual.championships.util.CasualConfig
import net.minecraft.server.MinecraftServer
import net.minecraft.world.scores.PlayerTeam
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class JsonDataManager: DataManager {
    override fun getParticipants(): CompletableFuture<Set<GameProfile>> {
        return CompletableFuture.completedFuture(setOf())
    }

    override fun createTeams(server: MinecraftServer): CompletableFuture<Collection<PlayerTeam>> {
        return CompletableFuture.completedFuture(listOf())
    }

    override fun reloadTeams(server: MinecraftServer): CompletableFuture<Void> {
        return CompletableFuture.completedFuture(null)
    }

    override fun syncUHCData(uhc: UHCMinigame) {
        this.syncMinigameData(uhc)
    }

    override fun syncDuelData(duel: DuelMinigame) {
        this.syncMinigameData(duel)
    }

    override fun close() {

    }

    private fun syncMinigameData(minigame: Minigame) {
        val serialized = minigame.data.toJson()
        CompletableFuture.runAsync {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
                val currentDate = format.format(Date())
                if (!stats.exists()) {
                    stats.createDirectories()
                }
                var path = stats.resolve("${minigame.id} $currentDate.json")
                if (path.exists()) {
                    path = stats.resolve("${minigame.id} (${minigame.uuid}) $currentDate.json")
                }
                path.bufferedWriter().use {
                    JsonUtils.encode(serialized, it)
                }
            } catch (e: Exception) {
                CasualMod.logger.error("Failed to write stats!", e)
                // So we have it somewhere!
                CasualMod.logger.error(JsonUtils.GSON.toJson(serialized))
            }
        }
    }

    private companion object {
        val stats = CasualConfig.resolve("stats")
    }
}