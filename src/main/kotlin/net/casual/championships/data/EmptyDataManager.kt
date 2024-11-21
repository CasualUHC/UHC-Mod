package net.casual.championships.data

import com.mojang.authlib.GameProfile
import net.casual.championships.duel.DuelMinigame
import net.casual.championships.uhc.UHCMinigame
import net.minecraft.server.MinecraftServer
import net.minecraft.world.scores.PlayerTeam
import java.util.concurrent.CompletableFuture

class EmptyDataManager: DataManager {
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

    }

    override fun syncDuelData(duel: DuelMinigame) {

    }

    override fun close() {

    }
}