package net.casual.championships.data

import com.mojang.authlib.GameProfile
import net.casual.championships.duel.DuelMinigame
import net.casual.championships.uhc.UHCMinigame
import net.minecraft.Util
import net.minecraft.server.MinecraftServer
import net.minecraft.world.scores.PlayerTeam
import java.util.concurrent.CompletableFuture

class MultiDataManager(
    private val managers: List<DataManager>
): DataManager {
    override fun getParticipants(): CompletableFuture<Set<GameProfile>> {
        return Util.sequenceFailFast(this.managers.map { it.getParticipants() })
            .thenApply { merged -> merged.flatMapTo(HashSet()) { it } }
    }

    override fun createTeams(server: MinecraftServer): CompletableFuture<Collection<PlayerTeam>> {
        return Util.sequenceFailFast(this.managers.map { it.createTeams(server) })
            .thenApply { merged -> merged.flatten() }
    }

    override fun reloadTeams(server: MinecraftServer): CompletableFuture<Void> {
        return CompletableFuture.allOf(*this.managers.map { it.reloadTeams(server) }.toTypedArray())
    }

    override fun syncUHCData(uhc: UHCMinigame) {
        for (manager in this.managers) {
            manager.syncUHCData(uhc)
        }
    }

    override fun syncDuelData(duel: DuelMinigame) {
        for (manager in this.managers) {
            manager.syncDuelData(duel)
        }
    }

    override fun close() {
        for (manager in this.managers) {
            manager.close()
        }
    }

    companion object {
        fun of(vararg managers: DataManager): MultiDataManager {
            return MultiDataManager(managers.toList())
        }
    }
}