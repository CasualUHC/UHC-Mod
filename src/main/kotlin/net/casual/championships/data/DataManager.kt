package net.casual.championships.data

import com.mojang.authlib.GameProfile
import net.casual.championships.duel.DuelMinigame
import net.casual.championships.uhc.UHCMinigame
import net.minecraft.server.MinecraftServer
import net.minecraft.world.scores.PlayerTeam
import java.util.concurrent.CompletableFuture

interface DataManager {
    fun getParticipants(): CompletableFuture<Set<GameProfile>>

    fun createTeams(server: MinecraftServer): CompletableFuture<Collection<PlayerTeam>>

    fun reloadTeams(server: MinecraftServer): CompletableFuture<Void>

    fun syncUHCData(uhc: UHCMinigame)

    fun syncDuelData(duel: DuelMinigame)

    fun close()
}