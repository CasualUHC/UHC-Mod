package net.casual.championships.resources

import com.google.common.collect.HashBiMap
import com.google.common.hash.Hashing
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import me.senseiwells.replay.config.ReplayConfig
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.server.ServerStoppingEvent
import net.casual.arcade.host.HostedPack
import net.casual.arcade.host.PackHost
import net.casual.arcade.host.PackHost.HostedPackRef
import net.casual.arcade.host.pack.DirectoryPackSupplier
import net.casual.arcade.resources.ArcadeResourcePacks
import net.casual.arcade.resources.creator.NamedResourcePackCreator
import net.casual.arcade.resources.utils.ResourcePackUtils.addPack
import net.casual.arcade.utils.TeamUtils.getHexColor
import net.casual.championships.CasualMod
import net.casual.championships.common.CommonMod
import net.casual.championships.events.CasualConfigReloaded
import net.casual.championships.uhc.UHCMod
import net.casual.championships.util.CasualConfig
import net.minecraft.ChatFormatting
import net.minecraft.world.scores.PlayerTeam
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

object CasualResourcePackHost {
    private val packs = CasualConfig.resolve("packs")
    private val generated = this.packs.resolve("generated")

    private val colors = Object2IntOpenHashMap<ChatFormatting>()

    private val host = PackHost(CasualMod.config.packHostIp, CasualMod.config.packHostPort)
    private val common = HashMap<String, HostedPackRef>()

    val uhc: HostedPack by this.host(UHCMod.UHC_PACK)

    init {
        this.host.addSupplier(DirectoryPackSupplier(this.packs))
        for (creator in CommonMod.COMMON_PACKS) {
            this.hostCommon(creator)
        }
    }

    fun getCommonPacks(): List<HostedPack> {
        return this.common.values.map(HostedPackRef::value)
    }

    @Deprecated("")
    fun getHostedPack(name: String): HostedPack? {
        return this.host.getHostedPack(name)
    }

    fun loadTeamColors(teams: Collection<PlayerTeam>): Boolean {
        var index = 0
        val colors = HashBiMap.create<ChatFormatting, Int>()
        for (team in teams) {
            val color = team.getHexColor()?.coerceIn(-1, 0xFFFFFF) ?: continue
            val original = colors.inverse()[color] ?: ChatFormatting.entries.getOrNull(index++)
            if (original == null) {
                CasualMod.logger.error("Tried to load more team colors than were available!!")
            } else {
                colors[original] = color
                team.color = original
            }
        }

        if (this.colors != colors) {
            this.colors.clear()
            this.colors.putAll(colors)
            this.hostCommon(ArcadeResourcePacks.createCustomGlowColorPack {
                for ((formatting, color) in colors) {
                    if (color != -1) {
                        set(formatting, color)
                    } else {
                        rainbow(formatting)
                    }
                }
            })
            return true
        }
        return false
    }

    internal fun reload(): CompletableFuture<Void> {
        return this.host.reload()
    }

    internal fun registerEvents() {
        this.host.start()

        GlobalEventHandler.register<ServerStoppingEvent> {
            this.host.stop()
        }
        GlobalEventHandler.register<CasualConfigReloaded> {
            this.reload()
        }
    }

    private fun hostCommon(creator: NamedResourcePackCreator) {
        this.common[creator.zippedName()] = this.host(creator)
    }

    private fun host(creator: NamedResourcePackCreator): HostedPackRef {
        val ref = this.host.addPack(this.generated, creator)
        ref.future.thenApply { pack ->
            try {
                @Suppress("DEPRECATION")
                val pathHash = Hashing.sha1().hashString(pack.url, StandardCharsets.UTF_8).toString()
                creator.getCreator().build(ReplayConfig.root.resolve("packs").resolve(pathHash))
            } catch (e: IOException) {
                CasualMod.logger.error("Failed to cache pack for replays", e)
            }
        }
        return ref
    }
}